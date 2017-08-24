angular
    .module('nzbhydraApp')
    .controller('StatsController', StatsController);

function StatsController($scope, $filter, StatsService, blockUI, localStorageService, $timeout, $window) {

    $scope.dateOptions = {
        dateDisabled: false,
        formatYear: 'yy',
        startingDay: 1
    };
    var initializingAfter = true;
    var initializingBefore = true;
    $scope.afterDate = moment().subtract(30, "days").toDate();
    $scope.beforeDate = moment().add(1, "days").toDate();
    $scope.foo = {
        includeDisabledIndexersInStats: localStorageService.get("includeDisabledIndexersInStats") !== null ? localStorageService.get("includeDisabledIndexersInStats") : false
    };
    updateStats();


    $scope.openAfter = function () {
        $scope.after.opened = true;
    };

    $scope.openBefore = function () {
        $scope.before.opened = true;
    };

    $scope.after = {
        opened: false
    };

    $scope.before = {
        opened: false
    };

    $scope.toggleIncludeDisabledIndexers = function () {
        localStorageService.set("includeDisabledIndexersInStats", $scope.foo.includeDisabledIndexersInStats);
        updateStats();
    };

    function updateStats() {
        blockUI.start("Updating stats...");
        var after = $scope.afterDate !== null ? $scope.afterDate : null;
        var before = $scope.beforeDate !== null ? $scope.beforeDate : null;
        $scope.statsLoadingPromise = StatsService.get(after, before, $scope.foo.includeDisabledIndexersInStats).then(function (stats) {
            $scope.setStats(stats);
            //Resize event is needed for the -perUsernameOrIp charts to be properly sized because nvd3 thinks the initial size is 0
            $timeout(function () {
                $window.dispatchEvent(new Event("resize"));
            }, 500);
        });

        blockUI.reset();


    }

    $scope.$watch('beforeDate', function () {
        if (initializingBefore) {
            initializingBefore = false;
        } else {
            updateStats();
        }
    });


    $scope.$watch('afterDate', function () {
        if (initializingAfter) {
            initializingAfter = false;
        } else {
            updateStats();
        }
    });

    $scope.onKeypress = function (keyEvent) {
        if (keyEvent.which === 13) {
            updateStats();
        }
    };

    $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
    $scope.format = $scope.formats[0];
    $scope.altInputFormats = ['M!/d!/yyyy'];

    $scope.setStats = function (stats) {
        stats = stats.data;

        $scope.nzbDownloads = null;
        $scope.avgResponseTimes = stats.avgResponseTimes;
        $scope.avgIndexerSearchResultsShares = stats.avgIndexerSearchResultsShares;
        $scope.indexerApiAccessStats = stats.indexerApiAccessStats;
        $scope.indexerDownloadShares = stats.indexerDownloadShares;
        $scope.downloadsPerHourOfDay = stats.downloadsPerHourOfDay;
        $scope.downloadsPerDayOfWeek = stats.downloadsPerDayOfWeek;
        $scope.searchesPerHourOfDay = stats.searchesPerHourOfDay;
        $scope.searchesPerDayOfWeek = stats.searchesPerDayOfWeek;
        $scope.downloadsPerAge = stats.downloadsPerAge;
        $scope.downloadsPerAgeStats = stats.downloadsPerAgeStats;
        $scope.successfulDownloadsPerIndexer = stats.successfulDownloadsPerIndexer;
        $scope.searchSharesPerUserOrIp = stats.searchSharesPerUserOrIp;
        $scope.downloadSharesPerUserOrIp = stats.downloadSharesPerUserOrIp;
        $scope.userAgentShares = stats.userAgentShares;
        $scope.numberOfConfiguredIndexers = stats.numberOfConfiguredIndexers;
        $scope.numberOfEnabledIndexers = stats.numberOfEnabledIndexers;


        var numIndexers = $scope.avgResponseTimes.length;

        $scope.avgResponseTimesChart = getChart("multiBarHorizontalChart", $scope.avgResponseTimes, "indexer", "avgResponseTime", "", "Response time");
        $scope.avgResponseTimesChart.options.chart.margin.left = 100;
        $scope.avgResponseTimesChart.options.chart.yAxis.rotateLabels = -30;
        var avgResponseTimesChartHeight = Math.max(numIndexers * 30, 350);
        $scope.avgResponseTimesChart.options.chart.height = avgResponseTimesChartHeight;

        $scope.resultsSharesChart = getResultsSharesChart();

        var rotation = 30;
        if (numIndexers > 30) {
            rotation = 70;
        }
        $scope.resultsSharesChart.options.chart.xAxis.rotateLabels = rotation;
        $scope.resultsSharesChart.options.chart.height = avgResponseTimesChartHeight;

        $scope.downloadsPerHourOfDayChart = getChart("discreteBarChart", $scope.downloadsPerHourOfDay, "hour", "count", "Hour of day", 'Downloads');
        $scope.downloadsPerHourOfDayChart.options.chart.xAxis.rotateLabels = 0;

        $scope.downloadsPerDayOfWeekChart = getChart("discreteBarChart", $scope.downloadsPerDayOfWeek, "day", "count", "Day of week", 'Downloads');
        $scope.downloadsPerDayOfWeekChart.options.chart.xAxis.rotateLabels = 0;

        $scope.searchesPerHourOfDayChart = getChart("discreteBarChart", $scope.searchesPerHourOfDay, "hour", "count", "Hour of day", 'Searches');
        $scope.searchesPerHourOfDayChart.options.chart.xAxis.rotateLabels = 0;

        $scope.searchesPerDayOfWeekChart = getChart("discreteBarChart", $scope.searchesPerDayOfWeek, "day", "count", "Day of week", 'Searches');
        $scope.searchesPerDayOfWeekChart.options.chart.xAxis.rotateLabels = 0;

        $scope.downloadsPerAgeChart = getChart("discreteBarChart", $scope.downloadsPerAge, "age", "count", "Downloads per age", 'Downloads');
        $scope.downloadsPerAgeChart.options.chart.xAxis.rotateLabels = 90;
        $scope.downloadsPerAgeChart.options.chart.showValues = false;

        $scope.successfulDownloadsPerIndexerChart = getChart("multiBarHorizontalChart", $scope.successfulDownloadsPerIndexer, "indexerName", "percentage", "Indexer", '% successful');
        $scope.successfulDownloadsPerIndexerChart.options.chart.xAxis.rotateLabels = 90;
        $scope.successfulDownloadsPerIndexerChart.options.chart.yAxis.tickFormat = function (d) {
            return $filter('number')(d, 0);
        };
        $scope.successfulDownloadsPerIndexerChart.options.chart.valueFormat = function (d) {
            return $filter('number')(d, 0);
        };
        $scope.successfulDownloadsPerIndexerChart.options.chart.showValues = true;

        $scope.indexerDownloadSharesChart = {
            options: {
                chart: {
                    type: 'pieChart',
                    height: 500,
                    x: function (d) {
                        return d.indexerName;
                    },
                    y: function (d) {
                        return d.share;
                    },
                    showLabels: true,
                    donut: true,
                    donutRatio: 0.35,
                    duration: 500,
                    labelThreshold: 0.03,
                    labelSunbeamLayout: true,
                    tooltip: {
                        valueFormatter: function (d, i) {
                            return $filter('number')(d, 2) + "%";
                        }
                    },
                    legend: {
                        margin: {
                            top: 5,
                            right: 35,
                            bottom: 5,
                            left: 0
                        }
                    }
                }
            },
            data: $scope.indexerDownloadShares
        };
        $scope.indexerDownloadSharesChart.options.chart.height = Math.min(Math.max(($scope.foo.includeDisabledIndexersInStats ? $scope.numberOfConfiguredIndexers : $scope.numberOfEnabledIndexers) * 40, 350), 900);

        function getSharesPieChart(data, height, xValue, yValue) {
            return {
                options: {
                    chart: {
                        type: 'pieChart',
                        height: height,
                        x: function (d) {
                            return d[xValue];
                        },
                        y: function (d) {
                            return d[yValue];
                        },
                        showLabels: true,
                        donut: true,
                        donutRatio: 0.35,
                        duration: 500,
                        labelThreshold: 0.03,
                        donutLabelsOutside: true,
                        //labelType: "percent",
                        labelSunbeamLayout: true,
                        tooltip: {
                            valueFormatter: function (d, i) {
                                return $filter('number')(d, 2) + "%";
                            }
                        },
                        legend: {
                            margin: {
                                top: 5,
                                right: 35,
                                bottom: 5,
                                left: 0
                            }
                        }
                    }
                },
                data: data
            };
        }

        if ($scope.searchSharesPerUserOrIp !== null) {
            $scope.downloadSharesPerUserOrIpChart = getSharesPieChart($scope.downloadSharesPerUserOrIp, 300, "userOrIp", "percentage");
            $scope.searchSharesPerUserOrIpChart = getSharesPieChart($scope.searchSharesPerUserOrIp, 300, "userOrIp", "percentage");
        }

        $scope.userAgentSharesChart = getSharesPieChart($scope.userAgentShares, 300, "userAgent", "percentage");
    };


    function getChart(chartType, values, xKey, yKey, xAxisLabel, yAxisLabel) {
        return {
            options: {
                chart: {
                    type: chartType,
                    height: 350,
                    margin: {
                        top: 20,
                        right: 20,
                        bottom: 100,
                        left: 50
                    },
                    x: function (d) {
                        return d[xKey];
                    },
                    y: function (d) {
                        return d[yKey];
                    },
                    showValues: true,
                    valueFormat: function (d) {
                        return d;
                    },
                    color: function () {
                        return "red"
                    },
                    showControls: false,
                    showLegend: false,
                    duration: 100,
                    xAxis: {
                        axisLabel: xAxisLabel,
                        tickFormat: function (d) {
                            return d;
                        },
                        rotateLabels: 30,
                        showMaxMin: false,
                        color: function () {
                            return "white"
                        }
                    },
                    yAxis: {
                        axisLabel: yAxisLabel,
                        axisLabelDistance: -10,
                        tickFormat: function (d) {
                            return d;
                        }
                    },
                    tooltip: {
                        enabled: false
                    },
                    zoom: {
                        enabled: true,
                        scaleExtent: [1, 10],
                        useFixedDomain: false,
                        useNiceScale: false,
                        horizontalOff: false,
                        verticalOff: true,
                        unzoomEventType: 'dblclick.zoom'
                    }
                }
            }, data: [{
                "key": "doesntmatter",
                "bar": true,
                "values": values
            }]
        };
    }

    //Was unable to use the function above for this and gave up
    function getResultsSharesChart() {
        return {
            options: {
                chart: {
                    type: 'multiBarChart',
                    height: 350,
                    margin: {
                        top: 20,
                        right: 20,
                        bottom: 100,
                        left: 45
                    },

                    clipEdge: true,
                    duration: 500,
                    stacked: false,
                    reduceXTicks: false,
                    showValues: true,
                    tooltip: {
                        enabled: true,
                        valueFormatter: function (d) {
                            return d + "%";
                        }
                    },
                    showControls: false,
                    xAxis: {
                        axisLabel: '',
                        showMaxMin: false,
                        rotateLabels: 30,
                        axisLabelDistance: 30,
                        tickFormat: function (d) {
                            return d;
                        }
                    },
                    yAxis: {
                        axisLabel: 'Share (%)',
                        axisLabelDistance: -20,
                        tickFormat: function (d) {
                            return d;
                        }
                    }
                }
            },

            data: [
                {
                    key: "Results",
                    values: _.map($scope.avgIndexerSearchResultsShares, function (stats) {
                        return {series: 0, y: stats.totalShare, x: stats.indexerName}
                    })
                },
                {
                    key: "Unique results",
                    values: _.map($scope.avgIndexerSearchResultsShares, function (stats) {
                        return {series: 1, y: stats.uniqueShare, x: stats.indexerName}
                    })
                }
            ]
        };
    }


}
