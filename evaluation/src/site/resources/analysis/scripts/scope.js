define(['jquery', 'util', 'theme', 'metric_themes' , 'highcharts'], function ($, util, theme, metric_themes) {

    var container = $('#chart');

    var metrics_array = Papa.parse(util.read(util.resultsPath + "metrics.csv"), {
        header: true
    }).data;

    var scenarios_array = Papa.parse(util.read(util.resultsPath + "scenarios.csv"), {
        header: true
    }).data;

    return{
        metrics: metrics_array,
        scenarios: scenarios_array,
        metric: {},
        matches: [],
        chart: {},
        data_type: 'overtime',
        renderer: function () {

            var chart_config;
            var themm = metric_themes[this.metric.name];
            if (themm == undefined) {
                themm = theme;
            }

            if (this.data_type == 'overtime') {
                var series = [];
                this.matches.forEach(function (match, index) {

                    var data = Papa.parse(util.read(util.analysisPath(match.name, this.metric.name + "_over_time.csv")), {header: true, dynamicTyping: true}).data;
                    var means = data.map(function (record) {

                        var x = util.convert.secondToHour(record.time * 10);
                        var y;

                        if (this.metric.type == 'Gauge') {
                            y = record.value_mean;
                        }
                        if (this.metric.type == 'Counter') {
                            y = record.counter_mean;
                        }
                        if (this.metric.type == 'Rate') {
                            y = record.rate_mean;
                        }
                        if (this.metric.type == 'Sampler' || this.metric.type == 'Timer') {
                            y = record.overall_mean;
                        }

                        y = y == 'NaN' ? 0 : y;
                        if (themm.convert != undefined && themm.convert.y != undefined) {
                            y = themm.convert.y(y);
                        }
                        return [ x, y ]
                    }, this);
                    var intervals = data.map(function (record) {
                        var x = util.convert.secondToHour(record.time * 10);
                        var lower;
                        var upper;

                        if (this.metric.type == 'Gauge') {
                            lower = record.value_ci_lower;
                            upper = record.value_ci_upper;
                        }
                        if (this.metric.type == 'Counter') {
                            lower = record.counter_ci_lower;
                            upper = record.counter_ci_upper;
                        }
                        if (this.metric.type == 'Rate') {
                            lower = record.rate_ci_lower;
                            upper = record.rate_ci_upper;
                        }
                        if (this.metric.type == 'Sampler' || this.metric.type == 'Timer') {
                            lower = record.overall_ci_lower;
                            upper = record.overall_ci_upper;
                        }
                        if (themm.convert != undefined && themm.convert.y != undefined) {
                            lower = themm.convert.y(lower);
                            upper = themm.convert.y(upper);
                        }
                        return [x, lower, upper]
                    }, this);
                    series.push({
                        name: match.churn,
                        data: means,
                        zIndex: 1,
                        lineWidth: 1,
                        color: theme.colours[index]
                    })
                    series.push({
                        name: match.churn,
                        type: 'arearange',
                        data: intervals,
                        fillOpacity: 0.3,
                        zIndex: 0,
                        lineWidth: 0,
                        linkedTo: match.name,
                        color: theme.colours[index]
                    });
                }, this);

                chart_config = {
                    series: series
                };
            }

            var group_by = 'churn'

            if (this.data_type == 'overall' | this.data_type == 'btraining' | this.data_type == 'atraining') {

                var series = [];
                var by_peer_configuration = this.matches.groupBy('peer_configuration');

                Object.keys(by_peer_configuration).forEach(function (key, index) {

                    var matches_by = by_peer_configuration[key];

                    var points = [];
                    var intervals = [];

                    matches_by.sortBy(group_by).forEach(function (match, index) {

                        var data = Papa.parse(util.read(util.analysisPath(match.name, this.metric.name + "_overall" + (this.data_type == 'btraining' ? '_before_training' : this.data_type == 'atraining' ? '_after_training' : '') + ".csv")), {header: true, dynamicTyping: true}).data[0];

                        var y = data.mean == 'NaN' ? 0 : data.mean;
                        var lower = data.ci_lower;
                        var upper = data.ci_upper;
                        if (themm.convert != undefined && themm.convert.y != undefined) {
                            y = themm.convert.y(y);
                            lower = themm.convert.y(lower);
                            upper = themm.convert.y(upper);
                        }

                        points.push({name: match[group_by].replace(' hrs', ''), y: y});
                        intervals.push([ lower, upper]);
                    }, this);


                    series.push({
                        name: key,
                        id: key,
                        data: points,
                        lineWidth: 1,
                        color: theme.colours[index],
                        dashStyle: 'dot'
//                        ,type: 'column'
                    })
                    series.push({
                        type: 'errorbar',
                        data: intervals,
                        linkedTo: key,
                        color: theme.colours[index]
                    });

                }, this);

                chart_config = {

                    chart: {
                        type: 'scatter'
                    },
                    xAxis: {
                        type: 'category',
                        title: {
                            text: group_by.replace('_', ' ').toTitleCase() + (group_by == 'training_duration' ? ' (Hours)' : '')
                        }
                    },
                    series: series
                };
            }

            if (this.data_type == 'batraining') {
                var series = [];
                var by_peer_configuration = this.matches.groupBy('peer_configuration');

                Object.keys(by_peer_configuration).forEach(function (key, index) {

                    var matches_by = by_peer_configuration[key];

                    var points = [];
                    var intervals = [];

                    matches_by.sortBy(group_by).forEach(function (match, index) {

                        var data = Papa.parse(util.read(util.analysisPath(match.name, this.metric.name + "_overall_before_training.csv")), {header: true, dynamicTyping: true}).data[0];

                        var y = data.mean == 'NaN' ? 0 : data.mean;
                        var lower = data.ci_lower;
                        var upper = data.ci_upper;
                        if (themm.convert != undefined && themm.convert.y != undefined) {
                            y = themm.convert.y(y);
                            lower = themm.convert.y(lower);
                            upper = themm.convert.y(upper);
                        }

                        points.push({name: match[group_by].replace(' hrs', ''), y: y});
                        intervals.push([lower, upper]);
                    }, this);


                    series.push({
                        name: key + ' During Training',
                        id: key,
                        data: points,
                        lineWidth: 1,
                        color: theme.colours[index],
                        dashStyle: 'dot',
                    })
                    series.push({
                        type: 'errorbar',
                        data: intervals,
                        linkedTo: key,
                        color: theme.colours[index]
                    });

                }, this);

                Object.keys(by_peer_configuration).forEach(function (key, index) {

                    var matches_by = by_peer_configuration[key];

                    var points = [];
                    var intervals = [];

                    matches_by.sortBy(group_by).forEach(function (match, index) {

                        var data = Papa.parse(util.read(util.analysisPath(match.name, this.metric.name + "_overall_after_training.csv")), {header: true, dynamicTyping: true}).data[0];

                        var y = data.mean == 'NaN' ? 0 : data.mean;
                        var lower = data.ci_lower;
                        var upper = data.ci_upper;
                        if (themm.convert != undefined && themm.convert.y != undefined) {
                            y = themm.convert.y(y);
                            lower = themm.convert.y(lower);
                            upper = themm.convert.y(upper);
                        }



                        points.push({name: match[group_by].replace(' hrs', ''), y: y});
                        intervals.push([lower, upper]);
                    }, this);


                    series.push({
                        name: key+ ' After Training',
                        id: key +  2,
                        data: points,
                        lineWidth: 1,
                        color: theme.colours[index + 2],
                        dashStyle: 'dot'
                    })
                    series.push({
                        type: 'errorbar',
                        data: intervals,
                        linkedTo: key+ 2,
                        color: theme.colours[index + 2]
                    });

                }, this);

                chart_config = {

                    chart: {
                        type: 'scatter'
                    },
                    xAxis: {
                        type: 'category',
                        title: {
                            text: group_by.replace('_', ' ').toTitleCase() + (group_by == 'training_duration' ? ' (Hours)' : '')
                        }
                    },
                    series: series
                };
            }


            var extend = themm.extend(chart_config);


            if (extend.title != undefined && extend.title.text != null) {
                $("#main_title").text(extend.title);
            }
            container.highcharts(extend);
            this.chart = container.highcharts();
        }
    }
})