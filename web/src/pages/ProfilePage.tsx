import { useState } from 'react';
import type { CashbackCategory, SavingGoal, UserProfile } from '../types/banking';
import { formatMoney } from '../utils/formatters';

interface ProfilePageProps { profile: UserProfile; goals: SavingGoal[]; cashbackCategories: CashbackCategory[]; onUpdate: (payload: Partial<UserProfile>) => void; }

function ProfilePage({ profile, goals, cashbackCategories, onUpdate }: ProfilePageProps) {
  const [name, setName] = useState(profile.fullName);
  return (
    <div className="page-grid">
      <section className="page-hero"><div><p className="eyebrow">демо-режим</p><h2>{profile.fullName}</h2><p>Личные данные, цели и настройки выбранного пользователя.</p></div></section>
      <div className="content__row">
        <section className="card"><h2>Данные</h2><div className="profile-list"><div><span>Email</span><strong>{profile.email}</strong></div><div><span>Телефон</span><strong>{profile.phone}</strong></div><div><span>Город</span><strong>{profile.city}</strong></div><div><span>Уровень</span><strong>{profile.cashbackLevel}</strong></div></div><div className="form form--page"><label>Имя<input value={name} onChange={(event) => setName(event.target.value)} /></label><button className="button-primary" onClick={() => onUpdate({ fullName: name })}>Сохранить</button></div></section>
        <section className="card"><h2>Цели</h2><div className="goal-list">{goals.map((goal) => <div key={goal.id} className="goal-card"><span className="goal-card__icon">{goal.icon}</span><div><strong>{goal.title}</strong><span>{formatMoney(goal.saved, goal.currency)} из {formatMoney(goal.target, goal.currency)}</span><div className="progress"><span style={{ width: `${Math.min(100, (goal.saved / goal.target) * 100)}%` }} /></div></div></div>)}</div></section>
      </div>
      <section className="card"><h2>Кэшбэк</h2><div className="cashback-grid">{cashbackCategories.map((item) => <div key={item.id} className={`cashback-card ${item.selected ? 'cashback-card--selected' : ''}`}><span>{item.icon}</span><strong>{item.title}</strong><p>{item.percent}%</p></div>)}</div></section>
    </div>
  );
}

export default ProfilePage;
