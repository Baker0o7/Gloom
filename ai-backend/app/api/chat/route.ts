import ZAI, { ChatMessage } from 'z-ai-web-dev-sdk';
import { NextRequest, NextResponse } from 'next/server';

// Cache the ZAI instance
let zaiInstance: Awaited<ReturnType<typeof ZAI.create>> | null = null;

async function getZAI() {
  if (!zaiInstance) {
    zaiInstance = await ZAI.create();
  }
  return zaiInstance;
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { messages, model, temperature, maxTokens } = body;

    if (!messages || !Array.isArray(messages)) {
      return NextResponse.json(
        { error: 'Messages array is required' },
        { status: 400 }
      );
    }

    const zai = await getZAI();

    // Transform messages to the format expected by z.ai
    const chatMessages: ChatMessage[] = messages.map((msg: { role: string; content: string }) => ({
      role: msg.role === 'system' ? 'assistant' : msg.role as 'user' | 'assistant',
      content: msg.content
    }));

    const completion = await zai.chat.completions.create({
      messages: chatMessages,
      stream: false,
      thinking: { type: 'disabled' },
    });

    // Get the first choice content safely
    const firstChoice = completion.choices?.[0];
    const responseContent = firstChoice?.message?.content || '';
    const responseRole = firstChoice?.message?.role || 'assistant';
    const finishReason = firstChoice?.finish_reason || 'stop';

    // Build response matching Kotlin data class structure
    const response = {
      id: `chat-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      created: Math.floor(Date.now() / 1000),
      model: model || 'z-ai-model',
      choices: [
        {
          index: 0,
          message: {
            role: responseRole,
            content: responseContent
          },
          finish_reason: finishReason
        }
      ],
      usage: {
        prompt_tokens: 0,
        completion_tokens: 0,
        total_tokens: 0
      }
    };

    return NextResponse.json(response);
  } catch (error: unknown) {
    console.error('Chat API error:', error);
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json(
      { error: `Failed to process chat: ${errorMessage}` },
      { status: 500 }
    );
  }
}

export async function GET() {
  return NextResponse.json({
    status: 'ok',
    message: 'Gloom AI Backend is running',
    models: [
      { id: 'default', displayName: 'Z.AI Default', publisher: 'Z.AI', description: 'Default AI model' }
    ]
  });
}
