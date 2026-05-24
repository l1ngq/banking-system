import { useState } from 'react';
import type { CashbackCategory, SavingGoal, UserProfile } from '../types/banking';
import { formatMoney } from '../utils/formatters';

interface ProfilePageProps {
  profile: UserProfile;
  goals: SavingGoal[];
  cashbackCategories: CashbackCategory[];
  onUpdate: (payload: Partial<UserProfile>) => void;
}

function ProfilePage({ profile, goals, cashbackCategories, onUpdate }: ProfilePageProps) {
  const [name, setName] = useState(profile.fullName);
  const [phone, setPhone] = useState(profile.phone);
  const [city, setCity] = useState(profile.city);

  return (
    <div className="page-grid">
      <section className="page-hero">
        <div>
          <p className="eyebrow">профиль</p>
          <h2>{profile.fullName || 'Пользователь'}</h2>
          <p>Личные данные аккаунта и настройки кабинета.</p>
        </div>
      </section>

      <div className="content__row">
        <section className="card">
          <h2>Данные</h2>
          <div className="profile-list">
            <div><span>Email</span><strong>{profile.email || 'Не указан'}</strong></div>
            <div><span>Телефон</span><strong>{profile.phone || 'Не указан'}</strong></div>
            <div><span>Город</span><strong>{profile.city || 'Не указан'}</strong></div>
            <div><span>Статус</span><strong>{profile.role === 'admin' ? 'Администратор' : 'Пользователь'}</strong></div>
          </div>

          <div className="form form--page">
            <label>
              Имя
              <input value={name} onChange={(event) => setName(event.target.value)} />
            </label>
            <label>
              Телефон
              <input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="Не указан" />
            </label>
            <label>
              Город
              <input value={city} onChange={(event) => setCity(event.target.value)} placeholder="Не указан" />
            </label>
            <button className="button-primary" onClick={() => onUpdate({ fullName: name, phone, city })}>Сохранить</button>
          </div>
        </section>

        <section className="card">
          <h2>Цели</h2>
          {goals.length === 0 ? (
            <p className="empty-text">Цели пока не добавлены.</p>
          ) : (
            <div className="goal-list">
              {goals.map((goal) => (
                <div key={goal.id} className="goal-card">
                  <span className="goal-card__icon">{goal.icon}</span>
                  <div>
                    <strong>{goal.title}</strong>
                    <span>{formatMoney(goal.saved, goal.currency)} из {formatMoney(goal.target, goal.currency)}</span>
                    <div className="progress"><span style={{ width: `${Math.min(100, (goal.saved / goal.target) * 100)}%` }} /></div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <section className="card">
        <h2>Кэшбэк</h2>
        {cashbackCategories.length === 0 ? (
          <p className="empty-text">Категории кэшбэка пока не выбраны.</p>
        ) : (
          <div className="cashback-grid">
            {cashbackCategories.map((item) => (
              <div key={item.id} className={`cashback-card ${item.selected ? 'cashback-card--selected' : ''}`}>
                <span>{item.icon}</span>
                <strong>{item.title}</strong>
                <p>{item.percent}%</p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default ProfilePage;
