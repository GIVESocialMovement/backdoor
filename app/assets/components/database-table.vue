<template>
  <div class="database-table">
    <a href="/">Back to the table list</a>
    <h1>{{ table.name }}</h1>
    <filter-control
        :all-columns="allColumns"
        :visible-columns.sync="internalVisibleColumns"
        :filters.sync="internalFilters"
        :loading="loading"
        v-on:submit="submit"></filter-control>
    <div class="database-table__summary">
      Found {{ total }} records,
      <a :href="'/table/' + table.name + '/new'" v-if="table.canCreate">Add a row</a>
      <span v-else class="database-table__summary__add-row-disabled" title="You don't have the permission to create a new row.">Add a row</span>
      ,
      page:
      <a class="database-table__summary__page" :class="{ 'database-table__summary__page--current': (page == index) }" v-for="index in pages" :href="'/table/' + table.name + '/' + index + queryString">{{ index }}</a>
    </div>
    <table border="0" cellpadding="0" cellspacing="2" class="database-table__table">
      <tr>
        <th class="database-table__table__header delete-column" v-if="table.canDelete"></th>
        <th class="database-table__table__header" v-for="column in visibleColumns">
          <i v-if="column.canRead" class="fas database-table__table__header__sort" :class="getSortClass(column)" v-on:click.stop="toggleSort(column)"></i>
          <i v-else class="fas fa-ban database-table__table__header__sort-disabled" title="You are not allowed to read this column"></i>
          {{ column.name }}
        </th>
      </tr>
      <tr v-for="row in internalRows" :key="row.primaryKey.value" v-on:click="toggleHighlight(row)" :class="{ 'database-table__table__row--highlighted': row.highlighted }">
        <td class="database-table__table__cell delete-column" v-if="table.canDelete">
          <span v-if="row.loading" class="spinner"></span>
          <i v-else class="fas fa-trash-alt database-table__table__cell__delete" v-on:click="remove(row)"></i>
        </td>
        <td class="database-table__table__cell" v-for="field in row.fields" :key="field.column.name" v-on:click="copy(field)">
          <i class="fas fa-ban database-table__table__cell__edit-disabled" v-if="field.column.isAutoIncremental" title="This field is auto-incremental. Therefore, it's not editable."></i>
          <i class="fa fa-edit database-table__table__cell__edit" v-else-if="canEdit(field)" v-on:click.stop.prevent="edit(field, row)"></i>
          <i class="fas fa-ban database-table__table__cell__edit-disabled" v-else title="You are not allowed to edit this value"></i>
          <i v-if="field.value === undefined || field.value === null" class="database-table__table__cell__special">NULL</i>
          <i v-else-if="field.value === ''" class="database-table__table__cell__special">Empty-string</i>
          <span v-else class="database-table__table__cell__content" v-html="formatValue(field)"></span>
        </td>
      </tr>
    </table>
    <div class="database-table__notice" v-show="notice">{{ notice }}</div>
    <edit-panel :table="table" ref="editPanel"></edit-panel>
  </div>
</template>

<script>
  import FilterControl from './database-table/filter-control.vue'
  import EditPanel from './database-table/edit-panel.vue'

  Vue.component('filter-control', FilterControl);
  Vue.component('edit-panel', EditPanel);

  var chr = { '"': '&quot;', '&': '&amp;', '<': '&lt;', '>': '&gt;' };

  export default {
    name: 'database-table',
    props: {
      table: {
        type: Object,
        required: true
      },
      allColumns: {
        type: Array,
        required: true
      },
      visibleColumns: {
        type: Array,
        required: true
      },
      rows: {
        type: Array,
        required: true
      },
      filters: {
        type: Array,
        required: true
      },
      sorts: {
        type: Object,
        required: true
      },
      page: {
        type: Number,
        required: true
      },
      pageSize: {
        type: Number,
        required: true
      },
      total: {
        type: Number,
        required: true
      }
    },
    data: function() {
      let copiedOfVisibleColumns = [];
      for (let col of this.visibleColumns) {
        copiedOfVisibleColumns.push(col);
      }
      return {
        loading: false,
        internalFilters: this.filters,
        internalSorts: this.sorts,
        internalRows: this.rows,
        internalVisibleColumns: copiedOfVisibleColumns,
        notice: '',
        noticeId: 0
      };
    },
    computed: {
      columnByName: function() {
        let columnByName = {};
        for (let col of this.visibleColumns) {
          columnByName[col.name] = col;
        }
        return columnByName;
      },
      pages: function() {
        let count = parseInt(this.total / this.pageSize);
        if ((this.total % this.pageSize) > 0) {
          count++;
        }
        let pages = [];
        for (let i=1;i<=count;i++) {
          pages.push(i);
        }
        return pages;
      },
      queryString: function() {
        let queries = [];

        if (this.isVisibleColumnsSpecified()) {
          let cols = [];
          for (let col of this.internalVisibleColumns) {
            cols.push(encodeURIComponent(col.name));
          }
          queries.push(`columns=${cols.join(',')}`);
        }

        for (let filter of this.internalFilters) {
          let encodedColumn = encodeURIComponent(filter.column);
          let encodedValue = encodeURIComponent(filter.value);
          queries.push(`f-${encodedColumn}=${encodedValue}`);
        }

        let sorts = [];
        for (let col in this.internalSorts) {
          if (this.internalSorts.hasOwnProperty(col)) {
            let encodedValue = encodeURIComponent(`${col}.${this.internalSorts[col]}`);
            sorts.push(encodedValue);
          }
        }
        if (sorts.length > 0) {
          queries.push(`sort=${sorts.join(',')}`);
        }

        return '?' + queries.join('&');
      },
    },
    methods: {
      isVisibleColumnsSpecified: function() {
        if (this.internalVisibleColumns.length == 0) { return false; }
        if (this.allColumns.length != this.internalVisibleColumns.length) { return true; }

        for (let i=0;i<this.allColumns.length;i++) {
          if (this.allColumns[i].name != this.visibleColumns[i].name) {
            return true;
          }
        }

        return false;
      },
      canEdit: function(field) {
        return this.columnByName[field.column.name] && this.columnByName[field.column.name].canEdit;
      },
      formatValue: function(field) {
        let content = '' + field.renderedValue;

        if (content.length > 100) {
          return content.substring(0, 100) + '...';
        } else {
          return content;
        }
      },
      submit: function() {
        this.loading = true;
        window.location.href = this.queryString;
      },
      showNotice(text) {
        this.notice = text;
        this.noticeId++;
        let thisNoticeId = this.noticeId;

        setTimeout(
          () => {
            if (thisNoticeId == this.noticeId) {
              this.notice = '';
            }
          },
          1000
        );
      },
      toggleHighlight(row) {
        Vue.set(row, 'highlighted', !row.highlighted);
      },
      copy: function(field) {
        if (field.forbidden === true) {
          this.showNotice('You are not allowed to read this value.');
          return;
        }

        let text = field.value;
        if (text === null) {
          this.showNotice('The value is null and not copied to your clipboard.');
          return;
        } else if (text === '') {
          this.showNotice('The value is an empty string and not copied to your clipboard.');
          return;
        }

        let textArea = document.createElement("textarea");
        // Place in top-left corner of screen regardless of scroll position.
        textArea.style.position = 'fixed';
        textArea.style.top = 0;
        textArea.style.left = 0;

        // Ensure it has a small width and height. Setting to 1px / 1em
        // doesn't work as this gives a negative w/h on some browsers.
        textArea.style.width = '2em';
        textArea.style.height = '2em';

        // We don't need padding, reducing the size if it does flash render.
        textArea.style.padding = 0;

        // Clean up any borders.
        textArea.style.border = 'none';
        textArea.style.outline = 'none';
        textArea.style.boxShadow = 'none';

        // Avoid flash of white box if rendered for any reason.
        textArea.style.background = 'transparent';
        document.body.appendChild(textArea);
        textArea.value = '' + text;
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);

        this.showNotice('Copied.');
      },
      getSortClass: function(col) {
        if (this.internalSorts[col.name] == 'asc') {
          return 'fa-sort-up';
        } else if (this.internalSorts[col.name] == 'desc') {
          return 'fa-sort-down';
        } else {
          return 'fa-sort';
        }
      },
      toggleSort: function(col) {
        let direction = this.internalSorts[col.name];
        if (direction == 'asc') {
          Vue.delete(this.internalSorts, col.name);
        } else if (direction == 'desc') {
          Vue.set(this.internalSorts, col.name, 'asc');
        } else {
          Vue.set(this.internalSorts, col.name, 'desc');
        }
        this.loading = true;
        window.location.href = this.queryString;
      },
      edit: function(field, row) {
        this.$refs.editPanel.open(field, row);
      },
      remove: function(row) {
        if (!confirm(`Are you sure you want to delete the row (${row.primaryKey.column.name}=${row.primaryKey.value})?`)) {
          return;
        }

        row.loading = true;
        axios.post(
            `/table/${this.table.name}/delete`,
            {
              primaryKeyColumn: row.primaryKey.column.name,
              primaryKeyValue: row.primaryKey.value
            }
          )
          .then((resp) => {
            row.loading = false;
            for (let i = 0;i < this.rows.length; i++) {
              if (this.rows[i] == row) {
                this.rows.splice(i, 1);
              }
            }
          })
          .catch((error) => {
            row.loading = false;
            alert(error)
          });
      }
    }
  };
</script>

<style scoped lang="scss">
  .database-table {
    display: block;
    position: relative;

    .spinner {
      display: inline-block;
      box-sizing: border-box;
      vertical-align: middle;
      width: 15px;
      height: 15px;
      border-radius: 10px;
      border: 2px solid rgba(0, 0, 0, 0);
      border-top-color: #666;
      border-right-color: #666;
      border-left-color: #666;
      animation: spinner 1.1s linear infinite;
    }

    @keyframes spinner {
      to {transform: rotate(360deg);}
    }

    &__notice {
      position: fixed;
      bottom: 0px;
      left: 0px;
      right: 0px;
      padding: 10px;
      font-size: 16px;
      font-weight: 600;
      color: #fff;
      background-color: rgba(51, 51, 51, 0.9);
      text-align: center;
    }

    &__summary {
      padding: 10px 0px;

      &__add-row-disabled {
        cursor: not-allowed;
        text-decoration: underline;
        color: #aaa;
      }

      &__page {
        padding: 0px 5px;

        &--current {
          text-decoration: none;
          color: #333;
          font-weight: bold;
        }
      }
    }

    &__table {
      border-collapse: collapse;

      &__header {
        font-weight: bold;
        padding: 5px 5px 5px 25px;
        margin: 5px;
        background-color: #666;
        color: #fff;
        border: 1px solid #444;
        position: relative;
        text-align: left;

        &__sort, &__sort-disabled {
          position: absolute;
          top: 6px;
          left: 6px;
          width: 16px;
          height: 16px;
          cursor: pointer;
          color: #ccc;

          &:hover {
            color: #fff;
          }
        }

        &__sort-disabled {
          cursor: not-allowed;
        }
      }

      &__row {
        &--highlighted {
          background-color: #eee;
        }
      }

      .delete-column {
        padding-left: 5px;
        text-align: center;
      }

      &__cell {
        border: 1px solid #444;
        padding: 5px 5px 5px 25px;
        position: relative;
        min-width: 14px;
        vertical-align: top;


        &__special {
          font-size: 12px;
          font-weight: bolder;
          color: #aaa;
        }

        &__delete {
          width: 16px;
          height: 16px;
          cursor: pointer;
          color: #aaa;

          &:hover {
            color: #666;
          }
        }

        &__edit, &__edit-disabled {
          position: absolute;
          top: 5px;
          left: 4px;
          width: 16px;
          height: 16px;
          cursor: pointer;
          color: #aaa;

          &:hover {
            color: #666;
          }
        }

        &__edit-disabled {
          cursor: not-allowed;
        }
      }
    }
  }
</style>