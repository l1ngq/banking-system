import type {
  AssistantMessage,
  BankCard,
  BankingState,
  CashbackCategory,
  CurrencyRate,
  NavItem,
  NewsItem,
  SavingGoal,
  UserProfile,
} from '../types/banking';
import { getDisplayNameByEmail } from '../api/config';

export const navItems: NavItem[] = [
  { key: 'dashboard', label: 'Главная', icon: '⌂' },
  { key: 'accounts', label: 'Счета', icon: '₽' },
  { key: 'payments', label: 'Платежи', icon: '↗' },
  { key: 'currency', label: 'Валюта', icon: '$' },
];

export const currencyRates: CurrencyRate[] = [];
export const news: NewsItem[] = [];

const emptyCards: BankCard[] = [];
const emptyMessages: AssistantMessage[] = [];
const emptyGoals: SavingGoal[] = [];
const emptyCashback: CashbackCategory[] = [];

export function createBankingStateForEmail(email?: string | null): BankingState {
  const normalizedEmail = email?.trim().toLowerCase() ?? '';

  const profile: UserProfile = {
    fullName: getDisplayNameByEmail(normalizedEmail, 'Пользователь'),
    phone: '',
    email: normalizedEmail,
    role: 'user',
    theme: 'light',
    cashbackLevel: '',
    city: '',
  };

  return {
    profile,
    accounts: [],
    cards: emptyCards,
    transactions: [],
    rates: currencyRates,
    news,
    assistantMessages: emptyMessages,
    goals: emptyGoals,
    cashbackCategories: emptyCashback,
  };
}
