require.config(
    {
        config: {
            'waitSeconds': 30
        },
        baseUrl: 'scripts',
        paths: {
            jquery: 'lib/jquery-1.11.0',
            mark: 'lib/markup-1.5.18',
            json: 'lib/json',
            bootstrap: 'lib/bootstrap-3.1.1',
            highcharts: 'lib/highcharts-4.0.1/highcharts',
            highcharts_more: 'lib/highcharts-4.0.1/highcharts-more',
            highcharts_exporting: 'lib/highcharts-4.0.1/modules/exporting',
            highcharts_3d: 'lib/highcharts-4.0.1/highcharts-3d',
            highcharts_csv: 'lib/highcharts-4.0.1/modules/export-csv',
            highcharts_draggable_legend: 'lib/highcharts-4.0.1/modules/draggable-legend-box',
            papa: 'lib/papaparse.min',
            jstat: 'lib/jstat.min'
        },
        shim: {
            bootstrap: {
                deps: ['jquery']
            },
            papa: {
                deps: ['jquery']
            },
            highcharts: {
                deps: ['jquery']
            },
            highcharts_more: {
                deps: ['jquery', 'highcharts']
            },
            highcharts_exporting: {
                deps: ['jquery', 'highcharts']
            },
            highcharts_3d: {
                deps: ['jquery', 'highcharts']
            },
            highcharts_csv: {
                deps: ['jquery', 'highcharts']
            },
            highcharts_draggable_legend: {
                deps: ['jquery', 'highcharts']
            }
        }
    }
)
require(
    [
        'jquery',
        'scope',
        'util',
        'query',
        'action',
        'mark',
        'bootstrap',
        'highcharts' ,
        'highcharts_more',
        'highcharts_exporting',
        'highcharts_3d',
        'highcharts_csv',
        'highcharts_draggable_legend',
    ],
    function ($, scope, util, query) {


        $("#chart_list").html(Mark.up(util.read("templates/sidebar.html"), {metrics: scope.metrics}));

        $("#churn_filters").html(Mark.up(util.read("templates/filter.html"), {labels: Object.keys(scope.scenarios.groupBy("churn")), property_name: 'churn'}));
        $("#workload_filters").html(Mark.up(util.read("templates/filter.html"), {labels: Object.keys(scope.scenarios.groupBy("workload")), property_name: 'workload'}));
        $("#peer_configuration_filters").html(Mark.up(util.read("templates/filter.html"), {labels: Object.keys(scope.scenarios.groupBy("peer_configuration")), property_name: 'peer_configuration'}));
        $("#experiment_duration_filters").html(Mark.up(util.read("templates/filter.html"), {labels: Object.keys(scope.scenarios.groupBy("experiment_duration")), property_name: 'experiment_duration'}));
        $("#training_duration_filters").html(Mark.up(util.read("templates/filter.html"), {labels: Object.keys(scope.scenarios.groupBy("training_duration")), property_name: 'training_duration'}));
        $("#clustering_algorithm_filters").html(Mark.up(util.read("templates/filter.html"), {labels: Object.keys(scope.scenarios.groupBy("clustering_algorithm")), property_name: 'clustering_algorithm'}));
        $("#feedback_enabled_filters").html(Mark.up(util.read("templates/filter.html"), {labels: Object.keys(scope.scenarios.groupBy("feedback_enabled")), property_name: 'feedback_enabled'}));
        query.init();
        $(window).bind('resize', function (e) {
            window.resizeEvt;
            $(window).resize(function () {
                clearTimeout(window.resizeEvt);
                window.resizeEvt = setTimeout(function () {
                    util.resizeElementHeight(document.getElementById("chart_list"));
                }, 250);
            });
        });


    }
);