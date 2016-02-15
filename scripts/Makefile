#!/usr/bin/make -f

PYTHON=python

all: best_points_all training_predictions plots tables

best_points_all: best_points best_points_default

best_points:
	$(PYTHON) get_best_points.py --config=$(CONFIG) > results-$(CONFIG).csv && $(PYTHON) results2sql.py --config=$(CONFIG) --insert results-$(CONFIG).csv

best_points_adaptive:
	$(PYTHON) get_best_points.py --config=$(CONFIG) --batches=10 > results-$(CONFIG).csv && $(PYTHON) results2sql.py --config=$(CONFIG) --insert results-$(CONFIG).csv

best_points_default:
	$(PYTHON) get_best_points_default.py CV && $(PYTHON) results2sql.py --config=$(CONFIG) --insert results_default.csv

trajectories:
	$(PYTHON) trajectories2sql.py --config=$(CONFIG) --create-table

training_predictions:
	$(PYTHON) generate_training_predictions.py --config=$(CONFIG)

sqlite_init: sqlite_create sqlite_populate

sqlite_create:
	$(PYTHON) results2sql.py --config=$(CONFIG) --create-tables

sqlite_create_adaptive:
	$(PYTHON) results2sql.py --config=$(CONFIG) --create-tables --adaptive

sqlite_populate:
	$(PYTHON) results2sql.py --config=$(CONFIG) --insert-datasets --insert-experiments

plots:
	$(PYTHON) make_plots.py --config=$(CONFIG)

boxplots:
	$(PYTHON) make_plots.py --config=$(CONFIG) --only-boxplots

cv_dps_plots:
	$(PYTHON) make_plots.py --config=$(CONFIG) --only-cv-dps

trajectories_plots:
	$(PYTHON) make_plots.py --config=$(CONFIG) --only-trajectories

signals_plots:
	$(PYTHON) make_plots.py --config=$(CONFIG) --only-signals

flows_plots:
	$(PYTHON) make_plots.py --config=$(CONFIG) --only-flows

tables:
	$(PYTHON) table_index.py --config=$(CONFIG)
	$(PYTHON) table_configurations.py --config=$(CONFIG)
	$(PYTHON) table_strategies.py --config=$(CONFIG)
	$(PYTHON) table_index_bootstrap.py --config=$(CONFIG)

# TODO clusters:
	
