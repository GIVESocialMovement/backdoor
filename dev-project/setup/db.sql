CREATE USER backdoor_dev_project_user WITH PASSWORD 'dev';
DROP DATABASE backdoor_dev_project;
CREATE DATABASE backdoor_dev_project;
GRANT ALL PRIVILEGES ON DATABASE backdoor_dev_project to backdoor_dev_project_user;

CREATE USER backdoor_dev_project_target_user WITH PASSWORD 'dev';
DROP DATABASE backdoor_dev_project_target;
CREATE DATABASE backdoor_dev_project_target;
GRANT ALL PRIVILEGES ON DATABASE backdoor_dev_project_target to backdoor_dev_project_target_user;
ALTER USER backdoor_dev_project_target_user WITH SUPERUSER;

