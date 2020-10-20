---
title: Helpful commands
---

## ORACLE DATABASE

### How to login using sqlplus
```
sqlplus

username: system
password: oracle

CONNECT SYS AS SYSDBA

password: oracle
```

### Grant and unlock user in oracle
```
GRANT CONNECT, RESOURCE TO APP_USER;
ALTER USER APP_USER IDENTIFIED BY APP_USER ACCOUNT UNLOCK;
```


<!--stackedit_data:
eyJoaXN0b3J5IjpbMTYwMDg4MTQwNSwxOTk4NDc5MzE3XX0=
-->