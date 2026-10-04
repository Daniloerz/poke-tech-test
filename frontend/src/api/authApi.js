import { apiRequest } from './client.js';

export function login(username, password) {
  return apiRequest('/auth/login', { method: 'POST', body: { username, password } });
}

export function register(username, password) {
  return apiRequest('/auth/register', { method: 'POST', body: { username, password } });
}
