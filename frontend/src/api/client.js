const API_BASE_URL = '/api/v1';
const TOKEN_KEY = 'accessToken';

export const tokenStorage = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
};

/** An error answered by the API. `problem` is the Problem Details body (detail, errors, localId...). */
export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail ?? `Request failed with status ${status}.`);
    this.status = status;
    this.problem = problem ?? {};
  }
}

let handleUnauthorized = () => {};

export function setUnauthorizedHandler(handler) {
  handleUnauthorized = handler;
}

export async function apiRequest(path, { method = 'GET', body } = {}) {
  const token = tokenStorage.get();
  const headers = {};
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, { detail: 'The server is not reachable. Try again later.' });
  }

  if (response.status === 204) {
    return null;
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    // An expired or invalid token: forget it so the user logs in again.
    if (response.status === 401 && token) {
      handleUnauthorized();
    }
    throw new ApiError(response.status, data);
  }
  return data;
}

/** Turns the `errors` list of a 400 response into `{ field: message }`. */
export function fieldErrors(error) {
  const errors = error?.problem?.errors ?? [];
  return Object.fromEntries(errors.map(({ field, message }) => [field, message]));
}
