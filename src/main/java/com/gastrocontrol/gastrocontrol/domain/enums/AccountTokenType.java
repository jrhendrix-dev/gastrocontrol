package com.gastrocontrol.gastrocontrol.domain.enums;

/**
 * Token purpose for account-related flows.
 */
public enum AccountTokenType {
    INVITE_SET_PASSWORD,
    PASSWORD_RESET,

    /**
     * Two-step email change confirmation.
     * Token is sent to the NEW email address.
     */
    EMAIL_CHANGE
}
