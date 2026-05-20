export type CurrencyCode = 'RUB' | 'USD' | 'EUR';
export type Page = 'dashboard' | 'accounts' | 'cards' | 'payments' | 'currency' | 'news' | 'assistant' | 'profile';
export type Action = 'topup' | 'transfer' | 'pay' | 'exchange' | 'openAccount';
export type Theme = 'light' | 'dark';

export interface NavItem { key: Page; label: string; icon: string; }
export interface Account { id: string; name: string; number: string; balance: number; currency: CurrencyCode; type: 'debit' | 'saving' | 'credit'; status: 'active' | 'closed'; interestRate?: number; openedAt: string; }
export interface BankCard { id: string; name: string; maskedNumber: string; paymentSystem: 'МИР' | 'Visa' | 'Mastercard'; balance: number; currency: CurrencyCode; expiresAt: string; color: 'yellow' | 'black' | 'platinum' | 'violet'; status: 'active' | 'blocked'; cashback: number; dailyLimit: number; }
export interface Transaction { id: string; title: string; category: string; amount: number; currency: CurrencyCode; createdAt: string; status: 'success' | 'pending' | 'failed'; accountId: string; type: 'income' | 'outcome' | 'transfer' | 'exchange'; }
export interface CurrencyRate { code: Exclude<CurrencyCode, 'RUB'>; name: string; buy: number; sell: number; change: number; }
export interface NewsItem { id: string; title: string; description: string; tag: string; publishedAt: string; readMinutes: number; }
export interface AssistantMessage { id: string; role: 'user' | 'assistant'; text: string; createdAt: string; }
export interface UserProfile { fullName: string; phone: string; email: string; role: 'user' | 'admin'; theme: Theme; cashbackLevel: string; city: string; }
export interface SavingGoal { id: string; title: string; target: number; saved: number; currency: CurrencyCode; icon: string; }
export interface CashbackCategory { id: string; title: string; percent: number; icon: string; selected: boolean; }
export interface BankingState { profile: UserProfile; accounts: Account[]; cards: BankCard[]; transactions: Transaction[]; rates: CurrencyRate[]; news: NewsItem[]; assistantMessages: AssistantMessage[]; goals: SavingGoal[]; cashbackCategories: CashbackCategory[]; }
