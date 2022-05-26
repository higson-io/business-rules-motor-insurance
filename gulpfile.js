var gulp = require('gulp');
var plumber = require('gulp-plumber');
var less = require('gulp-less');
var rename = require('gulp-rename');
var merge = require('merge-stream');

var paths = {
    less_index: 'less/styles.less',
    less: 'less/**/*.less',
    less_dest: 'src/main/webapp/styles'
}

gulp.task('prepare:libs', function () {
    var libStream = merge();
    var libs = [
        'angular',
        'angular-tooltips',
        'angular-ui-bootstrap',
        'bootstrap',
        'spin.js',
        'angular-spinner',
        'angular-loading-spinner'
    ];
    libs.forEach(function(lib) {
        libStream.add(
            gulp.src('node_modules/' + lib + '/**')
                .pipe(gulp.dest('src/main/webapp/vendors/' + lib))
        );
    });
    return libStream;
});

gulp.task('less', function() {
    return gulp.src(paths.less_index)
        .pipe(plumber())
        .pipe(less())
        .pipe(rename('styles.min.css'))
        .pipe(gulp.dest(paths.less_dest))
});

gulp.task('default', gulp.series('less', 'prepare:libs'));

gulp.task('watch', function() {
    gulp.watch(paths.less, ['less']);
});
