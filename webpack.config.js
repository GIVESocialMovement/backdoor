const path = require('path')
const webpack = require('webpack')
const SbtVuefyPlugin = require('./sbt-vuefy-plugin.js')

class FailOnWarningPlugin {
	apply(compiler) {
	  compiler.plugin("emit", (compilation, callback) => {
      const json = compilation.getStats().toJson();

      if (json.warnings.length > 0) {
        compilation.errors.push( new Error('There are some warnings. Please fix.') )
      }
      callback();
	  });
	}
}

module.exports = {
  output: {
    publicPath: '/assets',
    library: '[camel-case-name]',
    filename: '[name].js',
  },
  plugins: [
    new SbtVuefyPlugin(),
    new FailOnWarningPlugin()
  ],
  cache: true,
  bail: true,
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        options: {
          loaders: {
            'scss': [
              'vue-style-loader',
              'css-loader',
              'sass-loader'
            ]
          }
        }
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        loader: 'babel-loader'
      }
    ]
  },
  performance: {
    hints: 'error',
    maxAssetSize: 1500000,
    maxEntrypointSize: 1500000,
    assetFilter: function(assetFilename) {
      return assetFilename.endsWith('.js');
    }
  },
  devtool: ''
}

if (process.env.NODE_ENV === 'production') {
  module.exports.devtool = '';
  module.exports.performance.maxAssetSize = 250000;
  module.exports.performance.maxEntrypointSize = 250000;
  module.exports.plugins = (module.exports.plugins || []).concat([
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: '"production"'
      }
    }),
    new webpack.optimize.UglifyJsPlugin({
      sourceMap: false,
      cache: true,
      parallel: true,
      compress: {
        warnings: false
      }
    }),
    new webpack.LoaderOptionsPlugin({
      minimize: true
    })
  ])
}
