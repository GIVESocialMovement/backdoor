CREATE USER backdoor_example_project_dev_user WITH PASSWORD 'dev';
DROP DATABASE backdoor_example_project_dev;
CREATE DATABASE backdoor_example_project_dev;
GRANT ALL PRIVILEGES ON DATABASE backdoor_example_project_dev to backdoor_example_project_dev_user;

CREATE USER backdoor_example_project_target_user WITH PASSWORD 'dev';
DROP DATABASE backdoor_example_project_target;
CREATE DATABASE backdoor_example_project_target;
GRANT ALL PRIVILEGES ON DATABASE backdoor_example_project_target to backdoor_example_project_target_user;
ALTER USER backdoor_example_project_target_user WITH SUPERUSER;

