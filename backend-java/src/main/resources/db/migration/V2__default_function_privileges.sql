-- Global default: new functions are not executable by PUBLIC (and so not by anon).
-- The earlier IN SCHEMA form in V1 cannot remove the global PUBLIC default.
alter default privileges for role postgres
  revoke execute on functions from public;