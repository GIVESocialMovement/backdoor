<template>
  <button v-on:click="click()">
    <span class="spinner" :class="{ visible: loading }"></span>
    <slot></slot>
  </button>
</template>

<script>
  export default {
    props: {
      loading: {
        type: Boolean,
        default: false
      }
    },
    methods: {
      click: function() {
        this.$emit('click');
      }
    }
  };
</script>

<style scoped lang="scss">
  button {
    cursor: pointer;
  }

  .visible {
    display: inline-block !important;
  }

  @keyframes spinner {
    to {transform: rotate(360deg);}
  }

  .spinner {
    display: none;
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
</style>
