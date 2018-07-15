<template>
  <div class="history-table">
    <a href="/">Back to the table list</a>
    <h1>History</h1>
    <table border="0" cellpadding="0" cellspacing="2" class="history-table__table">
      <tr>
        <th>Performed at</th>
        <th>User</th>
        <th>Table</th>
        <th>Action</th>
      </tr>
      <tr v-for="item in items" :key="item.id">
        <td>{{ $formatTime(item.performedAt) }}</td>
        <td>{{ item.user.email }}</td>
        <td>{{ item.table }}</td>
        <td>{{ item.sql }}</td>
      </tr>
    </table>
    <a v-if="items.length > 0" :href="'/history?i=' + items[items.length - 1].id">Older entries</a>
  </div>
</template>

<script>
  export default {
    name: 'database-table',
    props: {
      items: {
        type: Array,
        required: true
      },
    }
  };
</script>

<style scoped lang="scss">
  .history-table {
    display: block;
    padding: 10px;
    position: relative;

    &__table {
      border-collapse: collapse;

      th {
        font-weight: bold;
        padding: 5px;
        margin: 5px;
        background-color: #666;
        color: #fff;
        border: 1px solid #444;
        position: relative;
        text-align: left;
      }


      td {
        border: 1px solid #444;
        padding: 5px;
        position: relative;
        min-width: 14px;
        vertical-align: top;
      }
    }
  }
</style>
