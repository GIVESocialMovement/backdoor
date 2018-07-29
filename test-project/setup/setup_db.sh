DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

psql -d postgres -U postgres -p 5432 -a -f ${DIR}/db.sql
psql -d backdoor_test_project_target -U backdoor_test_project_target_user -p 5432 -a -f ${DIR}/table.sql
