from scipy.cluster.hierarchy import dendrogram, linkage
import matplotlib.pyplot as plt

methods = ['single', 'complete', 'average', 'centroid', 'median', 'ward']

def plot_dendogram(distance_matrix, labels, title):
    for cluster_method in methods:
        cluster = linkage(distance_matrix, method=cluster_method)
        fig, ax = plt.subplots(figsize=(10, 15))  # set size
        plt.title(title)
        ax = dendrogram(cluster, labels=labels, orientation='right')

        plt.tight_layout()

        plt.savefig('../distances/%s.%s.png' % (title, cluster_method), dpi=200)
        plt.close("all")
