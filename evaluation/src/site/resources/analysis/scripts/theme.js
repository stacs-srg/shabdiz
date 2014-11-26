define(['jquery'], function ($) {

    return {
        colours: [
            '#3366cc', '#dc3912', '#ff9900', '#109618', '#990099', '#0099c6', '#dd4477',
            '#66aa00', '#b82e2e', '#316395', '#994499', '#22aa99', '#aaaa11', '#6633cc',
            '#e67300', '#8b0707', '#651067', '#329262', '#5574a6', '#3b3eac', '#b77322',
            '#16d620', '#b91383', '#f4359e', '#9c5935', '#a9c413', '#2a778d', '#668d1c',
            '#bea413', '#0c5922', '#743411'
        ],
        chart: {
            type: 'line',
            zoomType: 'xy',
            onePerMatch: false,
            plotBorderWidth: 1,
            plotBorderColor: 'black',

        },
        title: {
            text: null
        },
        exporting: {
            chartOptions: {
                title: {
                    text: null
                },
                chart: {
                    style: {
                        fontSize: '10pt',
                        fontFamily: '"Frutiger Light Condensed", sans-serif',
                    }
                }
            },
            sourceWidth: 500,
            sourceHeight: 350
        },
        legend: {
            itemStyle: {
                color: 'black',
                fontWeight: 'normal',
                fontSize: '10pt'
            },
            align: 'right',
            verticalAlign: 'top',
            layout: 'vertical',
            backgroundColor: 'white',
            borderWidth: 1,
            floating: true,
            draggable: true,
            zIndex: 20,
            x: -20,
            y: 20
        },

        plotOptions: {
            line: {
                marker: {
                    enabled: false
                }
            },
            arearange: {
                showInLegend: false
            },
            series: {
                animation: false,
                turboThreshold: 100
            }
        },
        tooltip: {

            headerFormat: '<b>{series.name}</b><br>',
            pointFormat: 'X: {point.x:.2f} , Y: {point.y:.2f}'
        },

        xAxis: {
            lineColor: 'black',
            tickWidth: 0,
            title: {
                text: 'Time Through Experiment (Hours)',
                style: {
                    color: 'black',
                    fontWeight: 'normal',
                    fontSize: '10pt'
                }
            },
            labels: {
                style: {
                    color: 'black',
                    fontWeight: 'normal',
                    fontSize: '10pt'
                }
            },
            min: 0
        },
        yAxis: {
            min: 0,
            title: {
                style: {
                    color: 'black',
                    fontWeight: 'normal',
                    fontSize: '10pt'
                }
            },
            labels: {
                style: {
                    color: 'black',
                    fontWeight: 'normal',
                    fontSize: '10pt'
                }
            },
        },
        series: [],
        credits: {
            enabled: false
        },
        convert: {
            y: undefined,
            x: undefined
        },
        extend: function (child) {
            var clone = $.extend(true, {}, this);
            $.extend(true, clone, this);
            return $.extend(true, clone, child);
        }
    }
});
