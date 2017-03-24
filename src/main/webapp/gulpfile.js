const gulp = require('gulp');
const plumber = require('gulp-plumber');
const less = require('gulp-less');
const rename = require('gulp-rename');

const paths = {
    less_index: 'less/styles.less',
    less: 'less/**/*.less',
    less_dest: 'styles'
}

gulp.task('less', function() {
    return gulp.src(paths.less_index)
        .pipe(plumber())
        .pipe(less())
        .pipe(rename('styles.min.css'))
        .pipe(gulp.dest(paths.less_dest))
});

gulp.task('default', ['less']);

gulp.task('watch', function() {
    gulp.watch(paths.less, ['less']);
});
