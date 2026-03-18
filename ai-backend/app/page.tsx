export default function Home() {
  return (
    <main style={{ padding: '2rem', fontFamily: 'system-ui, sans-serif' }}>
      <h1>Gloom AI Backend</h1>
      <p>AI service is running. Use POST /api/chat to interact.</p>
      <h2>Available Endpoints:</h2>
      <ul>
        <li>GET /api/chat - Health check</li>
        <li>POST /api/chat - Chat completions</li>
      </ul>
    </main>
  )
}
