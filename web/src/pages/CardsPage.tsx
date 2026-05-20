import type { BankCard } from '../types/banking';
import { formatMoney } from '../utils/formatters';

function CardsPage({ cards }: { cards: BankCard[] }) {
  return (
    <div className="page-grid">
      <section className="page-hero"><div><p className="eyebrow">демо-режим</p><h2>Карты</h2><p>Персональные карты выбранного пользователя в демо-режиме.</p></div></section>
      <div className="cards-page-grid">{cards.map((card) => <article key={card.id} className={`bank-card bank-card--${card.color} bank-card--large`}><div className="bank-card__top"><span>{card.name}</span><span>{card.paymentSystem}</span></div><strong>{card.maskedNumber}</strong><div className="bank-card__bottom"><span>{formatMoney(card.balance, card.currency)}</span><span>{card.expiresAt}</span></div></article>)}</div>
    </div>
  );
}

export default CardsPage;
