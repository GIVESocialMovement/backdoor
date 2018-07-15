CREATE USER backdoor_dev_user WITH PASSWORD 'dev';
CREATE DATABASE backdoor_dev;
GRANT ALL PRIVILEGES ON DATABASE backdoor_dev to backdoor_dev_user;

CREATE USER backdoor_test_user WITH PASSWORD 'test';
CREATE DATABASE backdoor_test;
GRANT ALL PRIVILEGES ON DATABASE backdoor_test to backdoor_test_user;
ALTER ROLE backdoor_test_user superuser;

CREATE USER backdoor_target_test_user WITH PASSWORD 'test';
CREATE DATABASE backdoor_target_test;
GRANT ALL PRIVILEGES ON DATABASE backdoor_target_test to backdoor_target_test_user;
ALTER ROLE backdoor_target_test_user superuser;
