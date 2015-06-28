#!/usr/bin/make -f

PYTHON=python

all: best_points_all training_predictions plots tables

best_points_all: best_points best_points_default best_points_random

best_points:
	$(PYTHON) get_best_points.py > results.csv && $(PYTHON) results2sql.py --insert results.csv

best_points_default:
	$(PYTHON) get_best_points_default.py CV && $(PYTHON) results2sql.py --insert results_default.csv

best_points_random:
	$(PYTHON) get_best_points_random.py > results_random.csv && $(PYTHON) results2sql.py --insert results_random.csv --convert-configuration

training_predictions:
	$(PYTHON) generate_training_predictions.py

sqlite_init: sqlite_create sqlite_populate

sqlite_create:
	$(PYTHON) results2sql.py --create-tables

sqlite_populate:
	$(PYTHON) results2sql.py --insert-datasets --insert-experiments

plots:
	$(PYTHON) make_plots.py

boxplots:
	$(PYTHON) make_plots.py --only-boxplots

cv_dps_plots:
	$(PYTHON) make_plots.py --only-cv-dps

trajectories_plots:
	$(PYTHON) make_plots.py --only-trajectories

signals_plots:
	$(PYTHON) make_plots.py --only-signals

flows_plots:
	$(PYTHON) make_plots.py --only-flows

tables:
	$(PYTHON) make_tables.py

clean: clean_plots clean_tables

clean_plots:
	rm ../plots/*.png

clean_tables:
	rm ../tables/*.html

clean_sqlite:
	rm results.db