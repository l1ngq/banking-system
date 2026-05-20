import { FormEvent, useState } from 'react';
import { quickPrompts } from '../data/mockData';
import type { AssistantMessage } from '../types/banking';
import { formatDateTime } from '../utils/formatters';

interface AssistantPageProps { messages: AssistantMessage[]; onSend: (text: string) => void; }

function AssistantPage({ messages, onSend }: AssistantPageProps) {
  const [text, setText] = useState('');
  function submit(event: FormEvent) { event.preventDefault(); if (!text.trim()) return; onSend(text); setText(''); }
  return (
    <div className="assistant-page">
      <section className="page-hero"><div><p className="eyebrow">демо-режим</p><h2>Ассистент</h2><p>Раздел показывает пример банковского помощника.</p></div></section>
      <div className="assistant-layout">
        <section className="chat">{messages.map((message) => <div key={message.id} className={`chat-message ${message.role === 'user' ? 'chat-message--user' : ''}`}><p>{message.text}</p><small>{formatDateTime(message.createdAt)}</small></div>)}<form className="chat-form" onSubmit={submit}><input value={text} onChange={(event) => setText(event.target.value)} placeholder="Спросить ассистента" /><button type="submit">Отправить</button></form></section>
        <aside className="card"><h2>Подсказки</h2><div className="prompt-list">{quickPrompts.map((prompt) => <button key={prompt} onClick={() => onSend(prompt)}>{prompt}</button>)}</div></aside>
      </div>
    </div>
  );
}

export default AssistantPage;
