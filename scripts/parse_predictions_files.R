library("RSQLite")
library("matrixStats")
library("ggplot2")
library("reshape")
library("Metrics")

###########################################
# FROM DATABASE
###########################################
con = dbConnect(RSQLite::SQLite(), dbname="C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\scripts\\results-adaptive.db")
dataset = "absorber"
sql_command = paste("select avg(test_error) from results where dataset LIKE '",dataset,"%' GROUP BY batch", sep="")

results_avg = dbGetQuery(con, sql_command)

sql_command = paste("select min(test_error) from results where dataset LIKE '",dataset,"%' GROUP BY batch", sep="")
results_min = dbGetQuery(con, sql_command)


###########################################
# FROM FILES
###########################################

datasets = c("absorber", "debutanizer", "catalyst", "IndustrialDrier", "oxeno", "sulfur", "ThermalOxidizerConv")

run_classification = function(datasets){
  
  for (d in 1:length(datasets)) {
    
    print(datasets[d])
    dataset = datasets[d]
    
    all_instances = matrix(0, 5, 25)
    all_test_error = matrix(0, 5, 25)
    
    test_error_adaptive = matrix(0, 25, 10)
    for (i in 0:24) {
      for (j in 0:9) {
        filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\adaptive\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
        #print(paste("Reading", filename))
        if (file.exists(filename)) {
          predictions = read.csv(filename)
          batch = as.data.frame(predictions)
          test_error_adaptive[i+1, j+1] = 100*sum(batch$error == "+", na.rm=TRUE)/nrow(batch)
          all_instances[1,i+1] = all_instances[1,i+1] + nrow(batch)
          all_test_error[1,i+1] = all_test_error[1,i+1] + sum(batch$error == "+", na.rm=TRUE)
        }
        else {
          print(paste("File", filename, "does not exists"))
          test_error_adaptive[i+1,j+1] = NaN
          all_instances[1,i+1] = NaN
          all_test_error[1,i+1] = NaN
        }
      }
    }
    
    test_error_retrain = matrix(0, 25, 10)
    for (i in 0:24) {
      for (j in 0:9) {
        filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\static\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
        #print(paste("Reading", filename))
        if (file.exists(filename)) {
          predictions = read.csv(filename)
          batch = as.data.frame(predictions)
          test_error_retrain[i+1, j+1] = 100*sum(batch$error == "+", na.rm=TRUE)/nrow(batch)
          all_instances[2,i+1] = all_instances[2,i+1] + nrow(batch)
          all_test_error[2,i+1] = all_test_error[2,i+1] + sum(batch$error == "+", na.rm=TRUE)
        }
        else {
          print(paste("File", filename, "does not exists"))
          test_error_retrain[i+1,j+1] = NaN
          all_instances[2,i+1] = NaN
          all_test_error[2,i+1] = NaN
        }
      }
    }
    
    test_error_static = matrix(0, 25, 10)
    for (i in 0:24) {
      filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\non-adaptive\\", dataset,"\\predictions.", i, ".csv", sep="")
      if (file.exists(filename)) {
        predictions = read.csv(filename)
        batches  = split(predictions, ceiling(seq_along(predictions$inst.)/(nrow(predictions)/10)))
        for (j in 1:10) {
          batch = as.data.frame(batches[j])
          test_error_static[i+1, j] = 100*sum(batch[4] == "+", na.rm=TRUE)/nrow(batch[4])
          all_instances[3,i+1] = all_instances[3,i+1] + nrow(batch[4])
          all_test_error[3,i+1] = all_test_error[3,i+1] + sum(batch[4] == "+", na.rm=TRUE)
        }
      }
      else {
        print(paste("File", filename, "does not exists"))
        test_error_static[i+1,] = NaN
        all_instances[3,i+1] = NaN
        all_test_error[3,i+1] = NaN
      }
    }
    
    test_error_incremental_adaptive = matrix(0, 25, 10)
    for (i in 0:24) {
      for (j in 0:9) {
        filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\incremental\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
        #print(paste("Reading", filename))
        if (file.exists(filename)) {
          predictions = read.csv(filename)
          batch = as.data.frame(predictions)
          test_error_incremental_adaptive[i+1, j+1] = 100*sum(batch$error == "+", na.rm=TRUE)/nrow(batch)
          all_instances[4,i+1] = all_instances[4,i+1] + nrow(batch)
          all_test_error[4,i+1] = all_test_error[4,i+1] + sum(batch$error == "+", na.rm=TRUE)
        }
        else {
          print(paste("File", filename, "does not exists"))
          test_error_incremental_adaptive[i+1,j+1] = NaN
          all_instances[4,i+1] = NaN
          all_test_error[4,i+1] = NaN
        }
      }
    }
    
    test_error_incremental_retrain = matrix(0, 25, 10)
    for (i in 0:24) {
      for (j in 0:9) {
        filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\incremental-static\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
        #print(paste("Reading", filename))
        if (file.exists(filename)) {
          predictions = read.csv(filename)
          batch = as.data.frame(predictions)
          test_error_incremental_retrain[i+1, j+1] = 100*sum(batch$error == "+", na.rm=TRUE)/nrow(batch)
          all_instances[5,i+1] = all_instances[5,i+1] + nrow(batch)
          all_test_error[5,i+1] = all_test_error[5,i+1] + sum(batch$error == "+", na.rm=TRUE)
        }
        else {
          print(paste("File", filename, "does not exists"))
          test_error_incremental_retrain[i+1,j+1] = NaN
          all_instances[5,i+1] = NaN
          all_test_error[5,i+1] = NaN
        }
      }
    }
    
    m1 = as.data.frame(test_error_adaptive)
    m1$id = "Batch + SMAC"
    m2 = as.data.frame(test_error_retrain)
    m2$id = "Batch"
    m3 = as.data.frame(test_error_static)
    m3$id = "Baseline"
    m4 = as.data.frame(test_error_incremental_adaptive)
    m4$id = "Incremental + SMAC"
    m5 = as.data.frame(test_error_incremental_retrain)
    m5$id = "Incremental"
    df = rbind(m1, m2, m3, m4, m5)
    
    
    g0 <- ggplot(data=melt(df, "id"), aes(as.factor(variable), value, colour=id, group=id))
    
    my.fun<-function(x){data.frame(ymin=min(x),ymax=max(x),y=mean(x))}
    g_box = g0 + 
      stat_summary(fun.data = mean_cl_boot, geom = "errorbar", width=0.2, size=0.5, alpha=0.5) + 
      stat_summary(fun.y = mean, geom = "line", aes(group=id, linetype=id), size=1.1) + 
      scale_linetype_manual(values=c("solid", "dashed", "dotted", "dashed", "dotted")) +
      theme_bw() + theme(panel.grid.major.x = element_line(colour = "gray"), panel.grid.major.y = element_blank(),
                         panel.grid.minor.x = element_blank(), panel.grid.major.y = element_blank()) +
      ylim(0,100) +
      xlab("Batch") + ylab("% class. error") + ggtitle(dataset)
    
    g_box
    ggsave(file=paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-adaptive\\", dataset, ".png", sep=""))
    
    
    names_error = c("Batch + SMAC", "Batch", "Baseline", "Incremental + SMAC", "Incremental")
    all_text = ""
    per_seed_text = ""
    per_seed_error = data.frame(NA, 5, 25)
    for(i in 1:5) {
      current_error = matrix(NA, 25, 1)
      for (j in 1:25) {
        current_error[j] = all_test_error[i,j]/all_instances[i,j]
        per_seed_error[i, j] = current_error[j]
        
      }
      all_text = sprintf("%s\n%s, %f", all_text, names_error[i], mean(current_error, na.rm=TRUE))
      per_seed_text = sprintf("%s\n%s, %f", per_seed_text, names_error[i], min(per_seed_error[i,], na.rm=TRUE))
    }
    write.csv(paste(all_text, per_seed_text, sep="\n"), file=paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-adaptive\\", dataset, ".csv", sep=""))
  }
}


###########################################
# FROM FILES -- REGRESSION
###########################################

datasets = c("absorber", "catalyst", "debutanizer", "IndustrialDrier", "oxeno", "sulfur", "ThermalOxidizerConv")

run_regression = function(datasets, metric_name) {
  
  metric = match.fun(metric_name)
  
  for (d in 1:length(datasets)) {
    
    print(datasets[d])
    dataset = datasets[d]
    
    all_instances = matrix(0, 5, 25)
    all_test_error = matrix(0, 5, 25)
    
    test_error_adaptive = matrix(0, 25, 10)
    for (i in 0:24) {
      for (j in 0:9) {
        filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\adaptive-regression\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
        #print(paste("Reading", filename))
        if (file.exists(filename)) {
          predictions = read.csv(filename)
          batch = as.data.frame(predictions)
          batch$error = as.numeric(batch$error)
          test_error_adaptive[i+1, j+1] = metric(as.numeric(batch$actual), as.numeric(batch$predicted))
          #all_instances[1,i+1] = all_instances[1,i+1] + nrow(batch)
          #all_test_error[1,i+1] = all_test_error[1,i+1] + sum(abs(as.numeric(batch$actual) - as.numeric(batch$predicted)))
        }
        else {
          print(paste("File", filename, "does not exists"))
          test_error_adaptive[i+1,j+1] = NaN
        }
      }
    }
    
    test_error_retrain = matrix(0, 25, 10)
    for (i in 0:24) {
      for (j in 0:9) {
        filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\static-regression\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
        #print(paste("Reading", filename))
        if (file.exists(filename)) {
          predictions = read.csv(filename)
          batch = as.data.frame(predictions)
          test_error_retrain[i+1, j+1] = metric(as.numeric(batch$actual), as.numeric(batch$predicted))
          #print(paste(i+1, j+1, test_error_retrain[i+1, j+1], batch$actual, sep=" "))
        }
        else {
          print(paste("File", filename, "does not exists"))
          test_error_retrain[i+1,j+1] = NaN
        }
      }
    }
    
    test_error_static = matrix(0, 25, 10)
    for (i in 0:24) {
      filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\non-adaptive-regression\\", dataset,"\\predictions.", i, ".csv", sep="")
      if (file.exists(filename)) {
        predictions = read.csv(filename)
        batches  = split(predictions, ceiling(seq_along(predictions$inst.)/(nrow(predictions)/10)))
        for (j in 1:10) {
          batch = as.data.frame(batches[j])
          names(batch) = c("inst", "actual", "predicted", "error")
          test_error_static[i+1, j] = metric(as.numeric(batch$actual), as.numeric(batch$predicted))
          #print(paste(i, j, test_error_static[i+1, j], batch[2], batch[3], sep=" "))
        }
      }
      else {
        print(paste("File", filename, "does not exists"))
        test_error_static[i+1,] = NaN
      }
    }
    
    
    m1 = as.data.frame(test_error_adaptive)
    m1$id = "Batch + SMAC"
    m2 = as.data.frame(test_error_retrain)
    m2$id = "Batch"
    m3 = as.data.frame(test_error_static)
    m3$id = "Baseline"
    df = rbind(m1, m2, m3)
    
    
    g0 <- ggplot(data=melt(df, "id"), aes(as.factor(variable), value, colour=id, group=id))
    
    my.fun<-function(x){data.frame(ymin=min(x),ymax=max(x),y=mean(x))}
    g_box = g0 + 
      stat_summary(fun.data = mean_cl_boot, geom = "errorbar", width=0.2, size=0.5, alpha=0.5) + 
      stat_summary(fun.y = mean, geom = "line", aes(group=id, linetype=id), size=1.1) + 
      scale_linetype_manual(values=c("solid", "dashed", "dotted")) +
      theme_bw() + theme(panel.grid.major.x = element_line(colour = "gray"), panel.grid.major.y = element_blank(),
                         panel.grid.minor.x = element_blank(), panel.grid.major.y = element_blank()) +
      xlab("Batch") + ylab(metric_name) + ggtitle(dataset)
    
    g_box
    ggsave(file=paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-adaptive-regression\\", dataset, ".", metric_name, ".png", sep=""))
    
    names_error = c("Batch + SMAC", "Batch", "Baseline")
    all_text = ""
    per_seed_text = ""
    per_seed_error = data.frame(NA, 5, 25)
    for(i in 1:5) {
      current_error = matrix(NA, 25, 1)
      for (j in 1:25) {
        current_error[j] = all_test_error[i,j]/all_instances[i,j]
        per_seed_error[i, j] = current_error[j]
        
      }
      all_text = sprintf("%s\n%s, %f", all_text, names_error[i], mean(current_error, na.rm=TRUE))
      per_seed_text = sprintf("%s\n%s, %f", per_seed_text, names_error[i], min(per_seed_error[i,], na.rm=TRUE))
    }
    write.csv(paste(all_text, per_seed_text, sep="\n"), file=paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-incremental\\", dataset, ".csv", sep=""))
  }
}

datasets = c("absorber", "catalyst", "debutanizer", "IndustrialDrier", "oxeno", "sulfur", "ThermalOxidizerConv")

plot_target_value = function(datasets) {
  for (d in 1:length(datasets)) {
    
    print(datasets[d])
    dataset = datasets[d]
    
    actual = c()
    predicted = c()
    
    for (batch in 0:9) {
      seed = 0
      filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\adaptive-regression\\",dataset,"\\batch",batch,"\\predictions.",seed,".csv", sep="")
      while (!file.exists(filename) && seed<25) {
        seed = seed+1
        filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\adaptive-regression\\",dataset,"\\batch",batch,"\\predictions.",seed,".csv", sep="")
      }
      data = as.data.frame(read.csv(filename))
      actual = append(actual, data$actual)
      predicted = append(predicted, data$predicted)
    }
    
    data = data.frame(1:length(actual), actual, predicted)
    names(data) = c("time", "actual", "predicted")
    
    #ggplot(data, aes(time)) + 
    #  geom_line(aes(y = actual, colour = "actual")) + 
    #  geom_line(aes(y = predicted, colour = "predicted"))
    
    
    g_box = ggplot(data, aes(time)) + 
      geom_line(aes(y = actual, colour = "actual")) +
      geom_vline(xintercept = seq(0, nrow(data), nrow(data)/10), colour="green", linetype = "longdash") + 
      theme_bw() + theme(panel.grid.major.x = element_blank(), panel.grid.major.y = element_blank(),
                         panel.grid.minor.x = element_blank(), panel.grid.major.y = element_blank())
    
    g_box
    ggsave(file=paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\plots-adaptive-regression\\", dataset, ".actual.batches.png", sep=""))
  }
}
