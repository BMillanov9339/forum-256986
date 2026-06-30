import { useCallback, useEffect, useState } from 'react'
import { login, logout } from './api/auth.js'
import { getAccessToken } from './api/client.js'
import {
  changePassword, changeUserRole, createReply, createTopic, createUser, currentUser,
  deleteReply, deleteTopic, deleteUser, listReplies, listTopics, listUsers, openTopic, purgeReply,
  purgeTopic, register, restoreStatus, setRestoreMode, updateProfile, updateReply, updateTopic,
} from './api/forum.js'
import './App.css'

const privileged = (user) => ['ADMIN', 'MODERATOR'].includes(user?.role)
const canEdit = (user, item) => user && (
  user.id === item.authorId ||
  user.role === 'ADMIN' ||
  (user.role === 'MODERATOR' && item.authorRole !== 'ADMIN')
)
const roleClass = (role) => role === 'ADMIN' ? 'admin-name' : role === 'MODERATOR' ? 'moderator-name' : ''
const moderationReason = () => {
  const reasonCode = window.prompt('Removal reason code (required for moderator actions)')
  if (!reasonCode?.trim()) return null
  const note = window.prompt('Optional note') ?? ''
  return { reasonCode: reasonCode.trim(), note: note.trim(), userNotified: false }
}

function App() {
  const [user, setUser] = useState(null)
  const [topics, setTopics] = useState({ items: [], page: 0, totalPages: 0, totalElements: 0 })
  const [selected, setSelected] = useState(null)
  const [view, setView] = useState('forum')
  const [authMode, setAuthMode] = useState('login')
  const [editingTopic, setEditingTopic] = useState(false)
  const [editingReply, setEditingReply] = useState(null)
  const [users, setUsers] = useState(null)
  const [restore, setRestore] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const showError = useCallback((value) => {
    setError(value?.message ?? 'Something went wrong')
    setNotice('')
  }, [])

  const refreshTopics = useCallback(async (page = 0) => {
    const result = await listTopics(page)
    setTopics(result)
  }, [])

  useEffect(() => {
    // Initial API synchronization belongs here; later refreshes are driven by user actions.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refreshTopics().catch(showError)
    if (getAccessToken()) currentUser().then(setUser).catch(() => logout())
  }, [refreshTopics, showError])

  async function run(action, success) {
    setError('')
    setNotice('')
    try {
      const result = await action()
      if (success) setNotice(success)
      return result
    } catch (value) {
      showError(value)
      throw value
    }
  }

  async function submitAuth(event) {
    event.preventDefault()
    const values = Object.fromEntries(new FormData(event.currentTarget))
    try {
      await run(async () => {
        if (authMode === 'register') {
          await register(values)
          await login(values.username, values.password)
        } else {
          await login(values.identifier, values.password)
        }
        setUser(await currentUser())
      })
    } catch { /* error is displayed by run */ }
  }

  function showTopicList() {
    setView('forum')
    setSelected(null)
    setEditingTopic(false)
    setEditingReply(null)
  }

  async function selectTopic(id, page = 0) {
    if (selected?.topic.id === id) return
    try {
      const details = await run(() => openTopic(id, page))
      setSelected(details)
      setView('forum')
      setEditingTopic(false)
      await refreshTopics(topics.page)
    } catch { /* handled */ }
  }

  async function refreshSelectedReplies(page = selected.replies.page, topic = selected.topic) {
    const replies = await run(() => listReplies(topic.id, page))
    setSelected({ topic, replies })
    await refreshTopics(topics.page)
  }

  async function submitTopic(event) {
    event.preventDefault()
    const form = event.currentTarget
    try {
      const topic = await run(() => createTopic(Object.fromEntries(new FormData(form))), 'Topic created.')
      form.reset()
      await refreshTopics(0)
      await selectTopic(topic.id)
    } catch { /* handled */ }
  }

  async function saveTopic(event) {
    event.preventDefault()
    const payload = Object.fromEntries(new FormData(event.currentTarget))
    try {
      const topic = await run(() => updateTopic(selected.topic.id, payload), 'Topic updated.')
      await refreshSelectedReplies(selected.replies.page, topic)
    } catch { /* handled */ }
  }

  async function removeTopic(purge = false) {
    const isModeratorAction = privileged(user) && user.id !== selected.topic.authorId
    const payload = (isModeratorAction || purge) ? moderationReason() : {}
    if ((isModeratorAction || purge) && !payload) return
    if (!window.confirm(purge ? 'Permanently purge this topic and its replies?' : 'Delete this topic?')) return
    try {
      await run(() => purge ? purgeTopic(selected.topic.id, payload) : deleteTopic(selected.topic.id, payload),
        purge ? 'Topic permanently purged.' : 'Topic deleted.')
      setSelected(null)
      await refreshTopics(topics.page)
    } catch { /* handled */ }
  }

  async function submitReply(event) {
    event.preventDefault()
    const form = event.currentTarget
    try {
      await run(() => createReply(selected.topic.id, new FormData(form).get('content')), 'Reply posted.')
      form.reset()
      await refreshSelectedReplies()
    } catch { /* handled */ }
  }

  async function saveReply(event, reply) {
    event.preventDefault()
    try {
      await run(() => updateReply(reply.id, new FormData(event.currentTarget).get('content')), 'Reply updated.')
      setEditingReply(null)
      await refreshSelectedReplies()
    } catch { /* handled */ }
  }

  async function removeReply(reply, purge = false) {
    const isModeratorAction = privileged(user) && user.id !== reply.authorId
    const payload = (isModeratorAction || purge) ? moderationReason() : {}
    if ((isModeratorAction || purge) && !payload) return
    if (!window.confirm(purge ? 'Permanently purge this reply?' : 'Delete this reply?')) return
    try {
      await run(() => purge ? purgeReply(reply.id, payload) : deleteReply(reply.id, payload),
        purge ? 'Reply permanently purged.' : 'Reply deleted.')
      await refreshSelectedReplies()
    } catch { /* handled */ }
  }

  async function submitProfile(event) {
    event.preventDefault()
    try {
      const updated = await run(() => updateProfile(Object.fromEntries(new FormData(event.currentTarget))), 'Profile updated.')
      setUser(updated)
    } catch { /* handled */ }
  }

  async function submitPassword(event) {
    event.preventDefault()
    const form = event.currentTarget
    try {
      await run(() => changePassword(Object.fromEntries(new FormData(form))), 'Password changed. Please sign in again.')
      form.reset()
      logout()
      setUser(null)
      setView('forum')
    } catch { /* handled */ }
  }

  async function loadUsers(page = 0) {
    try {
      setUsers(await run(() => listUsers(page)))
      if (user?.role === 'ADMIN') setRestore(await run(() => restoreStatus()))
      setView('users')
    } catch { /* handled */ }
  }

  async function setRole(target, role) {
    try {
      await run(() => changeUserRole(target.id, role), `${target.username} is now ${role.toLowerCase()}.`)
      await loadUsers(users.page)
    } catch { /* handled */ }
  }

  async function removeUser(target) {
    if (!window.confirm(`Anonymize ${target.username}? Their content will remain.`)) return
    try {
      await run(() => deleteUser(target.id), 'Account anonymized.')
      await loadUsers(users.page)
    } catch { /* handled */ }
  }

  async function submitCreateUser(event) {
    event.preventDefault()
    const form = event.currentTarget
    try {
      await run(() => createUser(Object.fromEntries(new FormData(form))), 'User created.')
      form.reset()
      await loadUsers(0)
    } catch { /* handled */ }
  }

  async function toggleRestore() {
    const enabling = !restore.restoreInProgress
    const reason = window.prompt(`${enabling ? 'Enable' : 'Disable'} restore mode: enter a reason`)
    if (!reason?.trim()) return
    if (enabling && !window.confirm('Restore mode will reject normal application traffic. Continue?')) return
    try {
      setRestore(await run(() => setRestoreMode(enabling, reason.trim()),
        `Restore mode ${enabling ? 'enabled' : 'disabled'}.`))
    } catch { /* handled */ }
  }

  return (
    <main>
      <header>
        <button className="brand" onClick={showTopicList}>Forum</button>
        <nav className="top-nav">
          <button onClick={showTopicList}>Topics</button>
          {user && <button onClick={() => setView('profile')}>Profile</button>}
          {privileged(user) && <button onClick={() => loadUsers()}>
            {user.role === 'ADMIN' ? 'Administration' : 'Users'}
          </button>}
        </nav>
        {user ? (
          <div className="account">
            <span>{user.username} · {user.role}</span>
            <button onClick={() => { logout(); setUser(null); setView('forum') }}>Sign out</button>
          </div>
        ) : (
          <button onClick={() => setAuthMode(authMode === 'login' ? 'register' : 'login')}>
            {authMode === 'login' ? 'Create account' : 'Sign in'}
          </button>
        )}
      </header>

      {error && <div className="message error">{error}</div>}
      {notice && <div className="message notice">{notice}</div>}

      {!user && (
        <form className="auth-card" onSubmit={submitAuth}>
          <h1>{authMode === 'login' ? 'Welcome back' : 'Create an account'}</h1>
          {authMode === 'register' ? <>
            <input name="username" placeholder="Username" maxLength="100" required />
            <input name="email" type="email" placeholder="Email" maxLength="320" required />
          </> : <input name="identifier" placeholder="Username or email" required />}
          <input name="password" type="password" placeholder="Password" minLength="8" maxLength="72" required />
          <button className="primary">{authMode === 'login' ? 'Sign in' : 'Register'}</button>
        </form>
      )}

      {view === 'profile' && user && (
        <section className="settings">
          <h1>Profile</h1>
          <form className="panel form-grid" onSubmit={submitProfile}>
            <h2>Account details</h2>
            <label>Username<input name="username" defaultValue={user.username} required /></label>
            <label>Email<input name="email" type="email" defaultValue={user.email ?? ''} required /></label>
            <button className="primary">Save profile</button>
          </form>
          <form className="panel form-grid" onSubmit={submitPassword}>
            <h2>Change password</h2>
            <label>Current password<input name="currentPassword" type="password" required /></label>
            <label>New password<input name="newPassword" type="password" minLength="8" maxLength="72" required /></label>
            <button className="primary">Change password</button>
          </form>
        </section>
      )}

      {view === 'users' && privileged(user) && users && (
        <section className="settings wide">
          <h1>User administration</h1>
          {user.role === 'ADMIN' && <form className="panel inline-form" onSubmit={submitCreateUser}>
            <div className="form-heading">
              <h2>Create account</h2>
              <p>Create a user, moderator, or administrator.</p>
            </div>
            <input name="username" placeholder="Username" required />
            <input name="email" type="email" placeholder="Email (optional)" />
            <input name="password" type="password" placeholder="Temporary password" minLength="8" maxLength="72" required />
            <select name="role" defaultValue="USER"><option>USER</option><option>MODERATOR</option><option>ADMIN</option></select>
            <button className="primary">Create user</button>
          </form>}
          {user.role === 'MODERATOR' && <div className="panel">
            Moderators may inspect users, but only administrators can create accounts or change roles.
          </div>}
          {user.role === 'ADMIN' && restore && <div className="panel operation-row">
            <div>
              <h2>Restore maintenance</h2>
              <p>Status: <strong>{restore.restoreInProgress ? 'Enabled' : 'Disabled'}</strong>. Retry interval: {restore.retryAfterSeconds} seconds.</p>
            </div>
            <button className={restore.restoreInProgress ? 'primary' : 'danger'} onClick={toggleRestore}>
              {restore.restoreInProgress ? 'Disable restore mode' : 'Enable restore mode'}
            </button>
          </div>}
          <div className="panel user-list">
            {users.items.map((target) => <div className="user-row" key={target.id}>
              <div><strong>{target.username}</strong><small>{target.email || 'No email'} · {target.role}</small></div>
              {user.role === 'ADMIN' && target.id !== user.id && <>
                {target.role !== 'ADMIN' && <select value={target.role} onChange={(e) => setRole(target, e.target.value)}>
                  <option>USER</option><option>MODERATOR</option>
                </select>}
                <button className="danger" onClick={() => removeUser(target)}>Anonymize</button>
              </>}
            </div>)}
          </div>
          <Pager page={users.page} totalPages={users.totalPages} onChange={loadUsers} />
        </section>
      )}

      {view === 'forum' && <section className="layout">
        <aside>
          <div className="section-title"><h2>Topics</h2><span>{topics.totalElements}</span></div>
          {topics.items.map((topic) => (
            <button className={`topic-row ${selected?.topic.id === topic.id ? 'active' : ''}`}
              key={topic.id} onClick={() => selectTopic(topic.id)}>
              <strong>{topic.title}</strong>
              <small><span className={roleClass(topic.authorRole)}>{topic.authorUsername}</span> · {topic.viewCount} views</small>
            </button>
          ))}
          <Pager page={topics.page} totalPages={topics.totalPages} onChange={(page) => refreshTopics(page)} />
        </aside>

        <article>
          {selected ? <>
            {editingTopic ? <form className="composer" onSubmit={saveTopic}>
              <input name="title" defaultValue={selected.topic.title} required />
              <textarea name="content" defaultValue={selected.topic.content} required />
              <div className="actions"><button className="primary">Save</button><button type="button" onClick={() => setEditingTopic(false)}>Cancel</button></div>
            </form> : <div className={`topic-head ${selected.topic.deleted ? 'deleted' : ''}`}>
              <p className="eyebrow"><span className={roleClass(selected.topic.authorRole)}>{selected.topic.authorUsername}</span> · {selected.topic.viewCount} views · {new Date(selected.topic.updatedAt).toLocaleString()}</p>
              <h1>{selected.topic.title}</h1>
              <p>{selected.topic.content}</p>
              {canEdit(user, selected.topic) && !selected.topic.deleted && <div className="actions">
                <button onClick={() => setEditingTopic(true)}>Edit</button>
                <button className="danger" onClick={() => removeTopic(false)}>Delete</button>
                {user.role === 'ADMIN' && <button className="danger" onClick={() => removeTopic(true)}>Purge</button>}
              </div>}
            </div>}
            <div className="replies">
              <h2>{selected.replies.totalElements} replies</h2>
              {selected.replies.items.map((reply) => <div className={`reply ${reply.deleted ? 'deleted' : ''}`} key={reply.id}>
                <strong className={roleClass(reply.authorRole)}>{reply.authorUsername}</strong><small>{new Date(reply.updatedAt).toLocaleString()}</small>
                {editingReply === reply.id ? <form className="composer compact" onSubmit={(event) => saveReply(event, reply)}>
                  <textarea name="content" defaultValue={reply.content} required />
                  <div className="actions"><button className="primary">Save</button><button type="button" onClick={() => setEditingReply(null)}>Cancel</button></div>
                </form> : <p>{reply.content}</p>}
                {canEdit(user, reply) && !reply.deleted && editingReply !== reply.id && <div className="actions">
                  <button onClick={() => setEditingReply(reply.id)}>Edit</button>
                  <button className="danger" onClick={() => removeReply(reply)}>Delete</button>
                  {user.role === 'ADMIN' && <button className="danger" onClick={() => removeReply(reply, true)}>Purge</button>}
                </div>}
              </div>)}
              <Pager page={selected.replies.page} totalPages={selected.replies.totalPages}
                onChange={(page) => refreshSelectedReplies(page)} />
              {user && !selected.topic.deleted && <form className="composer" onSubmit={submitReply}>
                <textarea name="content" placeholder="Write a reply…" maxLength="10000" required />
                <button className="primary">Reply</button>
              </form>}
            </div>
          </> : <div className="empty">
            <h1>Choose a topic</h1><p>Open a conversation, or start a new one.</p>
            {user && <form className="composer new-topic" onSubmit={submitTopic}>
              <input name="title" placeholder="Topic title" maxLength="500" required />
              <textarea name="content" placeholder="What would you like to discuss?" maxLength="10000" required />
              <button className="primary">Create topic</button>
            </form>}
          </div>}
        </article>
      </section>}
    </main>
  )
}

function Pager({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null
  return <nav className="pager">
    <button disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button>
    <span>Page {page + 1} of {totalPages}</span>
    <button disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
  </nav>
}

export default App
