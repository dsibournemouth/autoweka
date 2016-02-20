library("matrixStats")



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

dataset = "ThermalOxidizerConv"
test_error_adaptive = matrix(0, 25, 10)
for (i in 0:24) {
	for (j in 0:9) {
		filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\adaptive\\", dataset,"\\batch",j,"\\predictions.", i, ".csv", sep="")
		#print(paste("Reading", filename))
		if (file.exists(filename)) {
			predictions = read.csv(filename)
			batch = as.data.frame(predictions)
			test_error_adaptive[i+1, j+1] = 100*sum(batch$error == "+", na.rm=TRUE)/nrow(batch)
		}
		else {
			print(paste("File", filename, "does not exists"))
			test_error_adaptive[i+1,j+1] = NaN
		}
	}
}


test_error = matrix(0, 25, 10)
for (i in 0:24) {
	filename = paste("C:\\Users\\Manuel\\workspace-autoweka\\autoweka\\experiments\\non-adaptive\\", dataset,"\\predictions.", i, ".csv", sep="")
	if (file.exists(filename)) {
		predictions = read.csv(filename)
		batches  = split(predictions, ceiling(seq_along(predictions$inst.)/(nrow(predictions)/10)))
		for (j in 1:10) {
			batch = as.data.frame(batches[j])
			test_error[i+1, j] = 100*sum(batch[4] == "+", na.rm=TRUE)/nrow(batch[4])
		}
	}
	else {
		print(paste("File", filename, "does not exists"))
		test_error[i+1,] = NaN
	}
}


xrange = c(0,9)
yrange = c(0,100)
plot(xrange, yrange, type="n", xlab="Batch", ylab="Test error")
title(dataset)
colors = c("blue", "red")
ltype = c(1,2)

lines(0:9, colMeans(test_error_adaptive, na.rm=TRUE), type="b", col=colors[1], lty=ltype[1], lwd=2)
lines(0:9, colMins(test_error_adaptive, na.rm=TRUE), type="b", col=colors[1], lty=ltype[1], lwd=1)

lines(0:9, colMeans(test_error, na.rm=TRUE), type="b", col=colors[2], lty=ltype[2], lwd=2)
lines(0:9, colMins(test_error, na.rm=TRUE), type="b", col=colors[2], lty=ltype[2], lwd=1)

legend('topleft','groups', c("Yes","No"), lty=c(1,1), col=colors, ncol=2, bty ="n", lwd=2)

# TODO ggplot(meltdf,aes(x=Year,y=value,colour=variable,group=variable)) + stat_summary(fun.data = "mean_cl_boot", geom = "smooth")