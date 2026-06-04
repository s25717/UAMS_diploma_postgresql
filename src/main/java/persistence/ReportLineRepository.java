package persistence;

import model.ReportLine;

public class ReportLineRepository extends GenericRepository<ReportLine> {
    public ReportLineRepository() {
        super(ReportLine.class);
    }
}
