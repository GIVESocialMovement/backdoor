<template>
  <div class="new-row">
    <a :href="'/table/' + table">Back to the table <strong>{{ table }}</strong></a>
    <h1>Add a new row to {{ table }}</h1>
    <div class="new-row__fields">
      <table>
        <tr v-for="column in columns" :key="column.name" class="new-row__fields__row">
          <td class="new-row__fields__row__column-name">{{ column.name }}</td>
          <template v-if="column.isAutoIncremental">
            <td colspan="2" class="new-row__fields__row__disabled">
              This field is auto-incremental and not editable.
            </td>
          </template>
          <template v-else>
            <td><textarea class="new-row__fields__row__textarea" v-model="column.newValue" :disabled="column.newIsNull"></textarea></td>
            <td>
              <template v-if="column.isNullable">
                <input type="checkbox" :id="'isNull' + column.name" v-model="column.newIsNull" @change="isNullChanged(column)">
                <label :for="'isNull' + column.name">isNull</label>
              </template>
            </td>
          </template>
        </tr>
      </table>
    </div>
    <error-panel :error="error"></error-panel>
    <loading-button :loading="loading" v-on:click="submit()">Save</loading-button>
  </div>
</template>

<script>
  import LoadingButton from './common/loading-button.vue'
  import ErrorPanel from './common/error-panel.vue'

  Vue.component('loading-button', LoadingButton);
  Vue.component('error-panel', ErrorPanel);

  export default {
    props: {
      table: {
        type: String,
        required: true
      },
      columns: {
        type: Array,
        required: true
      },
    },
    data: function() {
      return {
        loading: false,
        error: null
      };
    },
    methods: {
      submit: function() {
        this.loading = true;

        let params = {};
        for (let column of this.columns) {
          if (column.isAutoIncremental) { continue; }

          params[column.name] = {
            value: column.newValue,
            isNull: column.newIsNull
          }
        }
        console.log(params);
        axios.post(`/table/${this.table}/create`, params)
          .then((resp) => {
            if (resp.data.success) {
              window.location.href = `/table/${this.table}`;
            } else {
              this.loading = false;
              console.log(error);
              alert(error);
            }
          })
          .catch((error) => {
            this.loading = false;
            this.error = error;
          });
      },
      isNullChanged: function(column) {
        this.$forceUpdate();
      }
    }
  };
</script>

<style scoped lang="scss">
  .new-row {
    display: block;
    position: relative;

    &__fields {
      display: block;

      &__row {
        &__disabled {
          font-size: 11px;
          color: #aaa;
        }

        &__column-name {
          text-align: right;
        }
      }
    }
  }
</style>
