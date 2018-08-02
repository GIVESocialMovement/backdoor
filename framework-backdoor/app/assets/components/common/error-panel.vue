<template>
  <div class="main" v-show="errors && errors.length > 0">
    <strong><slot></slot></strong>
    <ul>
      <li v-for="item in errors">
        {{ item.message }}
        <template v-if="item.trace">
          <pre>{{ item.trace }}</pre>
        </template>
      </li>
    </ul>
  </div>
</template>

<script>
  export default {
    props: {
      error: {
        type: Error,
        default: null
      }
    },
    computed: {
      errors: function() {
        if (!this.error) { return null; }

        if (!this.error.response) {
          return [{"message": "Something is wrong on the server. Please wait a few minutes and try again."}];
        }

        if (!this.error.response.data || !this.error.response.data.errors) {
          return [{"message": `Something is wrong (${this.error.response.status} ${this.error.response.statusText})`}];
        }

        return this.error.response.data.errors;
      }
    }
  };
</script>

<style scoped lang="scss">
  .main {
    display: block;
    margin: 5px 0px;
    color: #E41407;
    font-size: 12px;
    padding: 10px;
    background-color: #FFE8E8;
    border: 1px solid #D3999F;
    text-align: left;

    ul {
      padding: 0px 0px 0px 15px;
      margin: 5px 0px;
    }

    pre {
      font-size: 10px;
      font-weight: normal;
    }
  }
</style>
