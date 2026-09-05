# No models of its own: authentication orchestrates users.User + SimpleJWT.
# Refresh-token revocation uses rest_framework_simplejwt.token_blacklist
# (OutstandingToken/BlacklistedToken), which is the BSD's `refresh_tokens`
# table in practice — no need to hand-roll it.
