Backdoor
=================

[![CircleCI](https://circleci.com/gh/GIVESocialMovement/backdoor.svg?style=svg&circle-token=5a6a8be56d280b635d32252c95eed90a5f87a44e)](https://circleci.com/gh/GIVESocialMovement/backdoor)
[![codecov](https://codecov.io/gh/GIVESocialMovement/backdoor/branch/master/graph/badge.svg?token=DmQ8nPCjKF)](https://codecov.io/gh/GIVESocialMovement/backdoor)

Backdoor is a database modification tool for team. Here are its highlights:

* __History of modification:__ we track the data before modification and the modification itself.
* __Access control on columns:__ we can allow certain persons to edit (or only-read) certain columns.
* __Computed column:__ we can show an extra column which is computed on the existing columns. An example use case is showing a secret url computed from the id column and the key column.
* __Webhook:__ we can send webhook when certain data is edited. An example use case is updating the search index when a row is updated.

Backdoor is currently used at [GIVE.asia](https://give.asia) and only supports Postgresql for now.


Motivation
-----------

As [GIVE.asia](https://give.asia) have a small engineering team, one of the challenges that we have faced is that an admin dashboard, which is some form of CRUD, is needed in order to enable our team to modify data.

We've quickly realised that building an admin dashboard for a specific data model doesn't scale well. While other database tools (as listed [here](https://wiki.postgresql.org/wiki/Community_Guide_to_PostgreSQL_GUI_Tools#Postbird)) are okay, they estange non-technical users and lack of important collaboration-esque features (e.g. history and column-level access control help prevent mistakes).

Thus, Backdoor was born.
