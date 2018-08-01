<template>
  <div class="main">
    <div class="columns">
      <div class="shown">
        <strong>Show:</strong>
        <template v-if="visibleColumns.length > 0">
          <span class="col" v-for="(column, index) in visibleColumns" :key="column.name" v-on:click="removeColumn(index)">
            {{ column.name }}
          </span>
          <span class="remove-all-button" v-on:click="removeAllColumns()">Remove all</span>
        </template>
        <span class="remark" v-else>
          No column is selected to be visible.
          <template v-if="changed">Therefore, all columns will be shown</template>
          <template v-else>Therefore, all columns are shown.</template>
        </span>
      </div>
      <div class="hidden">
        <strong>Hide:</strong>
        <template v-if="hiddenColumns.length > 0">
          <span class="col" v-for="column in hiddenColumns" :key="column.name" v-on:click="addColumn(column)">
            {{ column.name }}
          </span>
          <span class="add-all-button" v-on:click="addAllColumns()">Add all</span>
        </template>
        <span class="remark" v-else>
          No column is selected to be hidden.
          <template v-if="changed">Therefore, all columns will be shown</template>
          <template v-else>Therefore, all columns are shown.</template>
        </span>
      </div>
    </div>
    <div class="filters">
      <div v-for="(filter, index) in filters" :key="index">
        {{ filter.column }} =
        <i v-if="filter.value == 'null'" class="special">NULL</i>
        <i v-else-if="filter.value == 'notnull'" class="special">NOT NULL</i>
        <i v-else-if="filter.value == 'v-'" class="special">Empty-string</i>
        <span v-else v-html="format(filter.value)"></span>
        <span class="delete-button" v-on:click="removeFilter(index)">delete</span>
      </div>
    </div>
    <div class="form">
      <select v-model="columnName">
        <option value="" selected>-- Select a column --</option>
        <option v-for="column in filterableColumns" :value="column.name">{{ column.name }}</option>
      </select>
      <input type="text" v-model="columnValue" :disabled="isNull">
      <input type="checkbox" id="isNull" v-model="isNull"><label for="isNull">isNull</label>
      <loading-button v-on:click="addFilter()">Add filter</loading-button>
    </div>
    <div class="submit" v-show="changed">
      <loading-button :loading="loading" v-on:click="submit()">Load new query</loading-button>
      <span class="remark">You have modified the filters. Click 'load new query' to see the result.</span>
    </div>
  </div>
</template>

<script>
  import LoadingButton from '../common/loading-button.vue'
  Vue.component('loading-button', LoadingButton);

  var chr = { '"': '&quot;', '&': '&amp;', '<': '&lt;', '>': '&gt;' };

  export default {
    props: {
      allColumns: {
        type: Array,
        required: true
      },
      visibleColumns: {
        type: Array,
        required: true
      },
      filters: {
        type: Array,
        default: function() {
          return [];
        }
      },
    },
    data: function() {
      return {
        columnName: '',
        columnValue: '',
        isNull: false,
        loading: false,
        changed: false,
      };
    },
    computed: {
      hiddenColumns: function() {
        let visibleColumns = {};
        for (let col of this.visibleColumns) {
          visibleColumns[col.name] = true;
        }

        let hiddenColumns = [];
        for (let col of this.allColumns) {
          if (!visibleColumns[col.name]) {
            hiddenColumns.push(col);
          }
        }

        return hiddenColumns;
      },
      filterableColumns: function() {
        let filterableCols = [];

        for (let col of this.allColumns) {
          if (col.filterable) {
            filterableCols.push(col);
          }
        }

        return filterableCols;
      }
    },
    methods: {
      removeAllColumns: function() {
        this.visibleColumns.splice(0, this.visibleColumns.length);
        this.changed = true;
      },
      addAllColumns: function() {
        let copied = [];
        for (let col of this.hiddenColumns) { copied.push(col); }

        for (let col of copied) { this.visibleColumns.push(col); }
        this.changed = true;
      },
      removeColumn: function(index) {
        this.visibleColumns.splice(index, 1);
        this.changed = true;
      },
      addColumn: function(column) {
        this.visibleColumns.push(column);
        this.changed = true;
      },
      format: function(value) {
        return ('' + value).substring(2).replace(/[\"&<>]/g, function (a) { return chr[a]; });
      },
      addFilter: function() {
        if (this.columnName.trim() == '') { return; }
        this.changed = true

        var value = this.columnValue;
        if (this.isNull) {
          value = 'null';
        } else {
          value = 'v-' + value;
        }

        this.filters.push({ column: this.columnName, value: value });

        this.columnName = '';
        this.columnValue = '';
        this.isNull = false;
      },
      removeFilter: function(index) {
        this.changed = true;
        this.filters.splice(index, 1);
      },
      submit: function() {
        this.$emit('submit');
      }
    }
  };
</script>

<style scoped lang="scss">
  .main {
    display: block;
    padding: 0px 0px 10px;

    input[type=text] {
      width: 300px;
    }

    .filters {
      display: block;

      .special {
        font-size: 12px;
        font-weight: bolder;
        color: #aaa;
      }

      .delete-button {
        margin-left: 20px;
        font-size: 11px;
        color: #666;
        cursor: pointer;
      }
    }
    .form { display: block; }
    .remark {
      font-size: 12px;
      color: #888;
      font-style: italic;
    }


    .columns {
      display: block;

      .shown {
        display: block;
        margin: 5px;

        span.col {
          padding: 3px 5px;
          background-color: #ccc;
          border-radius: 2px;
          text-align: center;
          margin: 0px 3px 3px 0px;
          display: inline-block;
          cursor: pointer;
        }

        .remove-all-button {
          display: block;
          font-size: 12px;
          color: #666;
          cursor: pointer;
        }
      }

      .hidden {
        display: block;
        margin: 5px;

        span.col {
          padding: 3px 5px;
          background-color: #ddd;
          color: #888;
          border-radius: 2px;
          text-align: center;
          margin: 0px 3px 3px 0px;
          display: inline-block;
          cursor: pointer;
        }

        .add-all-button {
          display: block;
          font-size: 12px;
          color: #666;
          cursor: pointer;
        }
      }
    }
  }
</style>
