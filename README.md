Backdoor
=================

[![CircleCI](https://circleci.com/gh/GIVESocialMovement/backdoor.svg?style=svg&circle-token=5a6a8be56d280b635d32252c95eed90a5f87a44e)](https://circleci.com/gh/GIVESocialMovement/backdoor)
[![codecov](https://codecov.io/gh/GIVESocialMovement/backdoor/branch/master/graph/badge.svg?token=DmQ8nPCjKF)](https://codecov.io/gh/GIVESocialMovement/backdoor)

Backdoor is a database modification tool for your team. It only supports Postgresql for now.

Here are its highlights:

* __History of modification:__ we track the data before modification and the modification.
* __Access control on columns:__ we can allow certain persons to edit (or only-read) certain columns.
* __Computed column:__ we can show an extra column which is computed on the existing columns. An example use case is showing a secret url computed from id and key.
* __Webhook:__ we can send webhook when certain data is edited. An example use case is updating the search index when a row is updated.


Backdoor is currently used at [GIVE.asia](https://give.asia)

Usage
------


