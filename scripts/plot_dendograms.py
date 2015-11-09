import os
import argparse
import numpy as np
from scipy.cluster.hierarchy import dendrogram, linkage
import matplotlib.pyplot as plt
from config import *

#linkage_methods = ['single', 'complete', 'average', 'centroid', 'median', 'ward']
linkage_methods = ['complete']

def plot_dendogram(distance_matrix, labels, title, folder):
    for cluster_method in linkage_methods:
        cluster = linkage(distance_matrix, method=cluster_method)
        fig, ax = plt.subplots(figsize=(10, 10))  # set size
        plt.title(title)
        ax = dendrogram(cluster, labels=labels, orientation='right')

        #plt.tight_layout()
       
        plt.gca().axes.get_xaxis().set_ticks([])

        plt.savefig('%s/%s.%s.svg' % (folder, title, cluster_method), dpi=65)
        plt.close("all")
        
        
def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--matrix', required=True)
    parser.add_argument('--method', choices=linkage_methods, required=False)
    
    args = parser.parse_args()
    
    selected_methods = [args.method] if args.method else linkage_methods
    folder, filename = os.path.split(args.matrix)
    
    distance_matrix = np.loadtxt(args.matrix)
    
    # TODO get labels
    labels = []
    plot_dendogram(distance_matrix, labels, filename, folder)
    

    
if __name__ == "__main__":
    main()
