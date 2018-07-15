<template>
  <div class="edit-panel" v-show="shouldShow" v-on:click="close($event)">
    <div class="edit-panel__dialog">
      <textarea ref="textBox" class="edit-panel__dialog__textarea" v-model="value" :disabled="isNull"></textarea><br/>
      <template v-if="field && field.column && field.column.isNullable">
        <input type="checkbox" id="isNullForEdit" v-model="isNull"><label for="isNullForEdit">isNull</label><br/>
      </template>
      <error-panel :error="error"></error-panel>
      <loading-button :loading="loading" v-on:click="submit()">Save</loading-button>
    </div>
  </div>
</template>

<script>
  import LoadingButton from '../common/loading-button.vue'
  import ErrorPanel from '../common/error-panel.vue'

  Vue.component('loading-button', LoadingButton);
  Vue.component('error-panel', ErrorPanel);

  export default {
    props: {
      table: {
        type: Object,
        required: true
      }
    },
    data: function() {
      return {
        field: null,
        row: null,
        value: '',
        isNull: false,
        loading: false,
        shouldShow: false,
        originalValue: '',
        error: null
      };
    },
    methods: {
      open: function(field, row) {
        this.field = field;
        this.row = row;
        if (field.value == null) {
          this.isNull = true;
          this.value = '';
        } else {
          this.isNull = false;
          this.value = field.editableValue;
        }
        this.originalValue = this.editableValue;
        this.error = null;
        this.shouldShow = true;

        Vue.nextTick(() => { this.$refs.textBox.select(); });
      },
      submit: function() {
        this.loading = true;
        this.error = null;
        axios.post(
            `/table/${this.table.name}/update`,
            {
              column: this.field.column.name,
              newValue: this.value,
              isNull: this.isNull,
              primaryKeyColumn: this.row.primaryKey.column.name,
              primaryKeyValue: this.row.primaryKey.value
            }
          )
          .then((resp) => {
            this.loading = false;
            for (let key in resp.data.field) {
              if (this.field.hasOwnProperty(key)) {
                Vue.set(this.field, key, resp.data.field[key]);
              }
            }
            Vue.nextTick(() => { this.shouldShow = false; });
          })
          .catch((error) => {
            this.loading = false;
            this.error = error;
          });
      },
      close: function(event) {
        if (event.target.classList.contains('edit-panel')) {
          if (this.originalValue != this.value && !confirm('Are you sure you want to close the edit dialog?')) {
            return;
          }
          this.shouldShow = false;
        }
      }
    }
  };
</script>

<style scoped lang="scss">
  .edit-panel {
    position: fixed;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    padding: 10px;
    font-size: 16px;
    font-weight: 600;
    background-color: rgba(51, 51, 51, 0.9);
    text-align: center;

    &__dialog {
      display: block;
      margin: 10px auto;
      background-color: #fefefe;
      border: 2px solid #ccc;
      padding: 20px;
      text-align: left;

      > * {
        margin: 5px;
      }

      &__textarea {
        width: 100%;
        min-height: 200px;
      }
    }

  }
</style>
