'use client';

import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import rehypeHighlight from 'rehype-highlight';

interface MarkdownViewerProps {
  content: string;
  [key: string]: any; // Allow other props to pass through
}

/**
 * Optimized Markdown viewer component that bundles all markdown
 * rendering dependencies together for lazy loading
 */
export default function MarkdownViewer({ content, ...props }: MarkdownViewerProps) {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm, remarkMath]}
      rehypePlugins={[rehypeKatex, rehypeHighlight]}
      {...props}
    >
      {content}
    </ReactMarkdown>
  );
}
