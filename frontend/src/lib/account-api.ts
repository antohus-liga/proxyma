import { apiRequest, type User } from '@/lib/api'

export type UpdateProfileInput = {
  display_name: string
}

export type ChangePasswordInput = {
  current_password: string
  new_password: string
}

export const accountApi = {
  updateProfile(input: UpdateProfileInput): Promise<User> {
    return apiRequest<User>('/users/me', {
      method: 'PATCH',
      body: input,
    })
  },

  changePassword(input: ChangePasswordInput): Promise<void> {
    return apiRequest<void>('/users/me/password', {
      method: 'POST',
      body: input,
    })
  },
}
