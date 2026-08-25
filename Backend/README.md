# Authentication Module - System Architecture

```text
                          AUTHENTICATION
                          │
          ┌───────────────┴────────────────┐
          │                                │
      REGISTER                           LOGIN
          │                                │
     Generate OTP                    Check password
          │                                │
     Email OTP                       Check verified
          │                                │
     Verify OTP                     Generate tokens
          │                                │
          └──────────────┬─────────────────┘
                         │
                  Access Token
                         +
                  Refresh Token
                         │
                         ▼
                  Authenticated
                    application
                         │
                         ▼
                  Refresh Token
                         │
              ┌──────────┴──────────┐
              │                     │
           valid                  expired
              │                     │
        rotate token             login again
              │
        new access token
        new refresh token


                  FORGOT PASSWORD
                         │
                         ▼
                    email lookup
                         │
                         ▼
                 Generate reset OTP
                         │
                         ▼
                    Send email
                         │
                         ▼
                  Verify reset OTP
                         │
                         ▼
                  Change password
                         │
                         ▼
              Revoke ALL refresh tokens
                         │
                         ▼
                    Login again
```
