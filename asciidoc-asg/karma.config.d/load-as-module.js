// Kotlin >= 2.4.20 emits wasmJs test bundles that use `import.meta` (e.g.
// `import.meta.resolve` in the generated wasm loader), which is only legal
// inside ES modules, while Karma includes scripts classically:
// "Uncaught SyntaxError: Cannot use 'import.meta' outside a module".
//
// Two adjustments, from a framework registered BEHIND karma-webpack so that
// its output chunks (injected into `files` at server start) are covered:
//  1. include every bundled script with type="module";
//  2. pin webpack's publicPath to the Karma URL of the webpack output dir.
//     The default ("auto") resolves via document.currentScript, which is
//     null inside module scripts, so the .wasm asset would 404 at the
//     server root. karma-webpack compiles lazily, after framework init, so
//     the controller's options are still mutable here.
config.plugins = config.plugins || [];
config.plugins.push({
    "framework:kotlin-esm-modules": ["factory", function (config) {
        config.files.forEach(function (file) {
            if (file && typeof file === "object" && file.included !== false &&
                    /\.m?js$/.test(file.pattern || "")) {
                file.type = "module";
            }
        });
        var controller = config.__karmaWebpackController;
        if (controller) {
            controller.webpackOptions.output.publicPath =
                "/absolute" + controller.outputPath + "/";
        }
    }],
});
config.frameworks = (config.frameworks || []).concat(["kotlin-esm-modules"]);
