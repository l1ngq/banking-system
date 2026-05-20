import type { NewsItem } from '../types/banking';
import { formatDate } from '../utils/formatters';

function NewsPage({ news }: { news: NewsItem[] }) {
  return (
    <div className="page-grid">
      <section className="page-hero"><div><p className="eyebrow">демо-режим</p><h2>Финансовые новости</h2><p>Раздел содержит демонстрационные финансовые материалы.</p></div></section>
      <div className="news-grid">{news.map((item) => <article key={item.id} className="news-card"><span>{item.tag}</span><h3>{item.title}</h3><p>{item.description}</p><small>{formatDate(item.publishedAt)} · {item.readMinutes} мин</small></article>)}</div>
    </div>
  );
}

export default NewsPage;
