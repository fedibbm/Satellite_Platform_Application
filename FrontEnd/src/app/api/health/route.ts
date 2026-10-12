import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json(
    {
      status: 'UP',
      service: 'satellite-frontend',
      timestamp: new Date().toISOString(),
    },
    { status: 200 }
  );
}
