export type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
};

export type RequestEmailChangeRequest = {
  newEmail: string;
  password: string;
};

export type ConfirmEmailChangeRequest = {
  token: string;
};
