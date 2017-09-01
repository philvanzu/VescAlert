package philvanzu.vescalert;

import android.os.Handler;

/**
 * Created by philippe on 24/08/2017.
 */

public class ParserThread extends Thread {

    private final Object lock = new Object();
    private boolean paused = false;

    private VescAlertService mService;


    public ParserThread(VescAlertService service)
    {
        mService = service;
    }

    public static final int FAIL_LogUnavailable = 0;

    @Override
    public void run() {
        boolean terminate = false;
        BtSnoopLogReader parser = new BtSnoopLogReader();
        parser.CreateDumpReader();
        if(!parser.opened)
        {
            ReportFailure(FAIL_LogUnavailable);
            return;
        }

        int loopcount = 0;
        while (!terminate)
        {
            try{
                synchronized (lock) {
                    if (!paused) {
                        VescStatus status = parser.ParseAvailable();
                        if(status == null && parser.errorCode != 0)
                        {
                            if(parser.errorCode == -1) {
                                if(parser.opened) parser.CloseDumpReader();
                                parser = new BtSnoopLogReader();
                                parser.CreateDumpReader();
                                status = parser.ParseAvailable();
                                if(status == null)
                                {
                                    ReportFailure(FAIL_LogUnavailable);
                                    if(parser.opened)parser.CloseDumpReader();
                                    return;
                                }
                            }
                            else if(parser.errorCode == -2) {
                                ReportFailure(FAIL_LogUnavailable);
                                terminate = true;
                            }
                            parser.errorCode = 0;
                        }
                         else ReportUpdate(status);
                    }
                }
                Thread.sleep(333);
            } catch(InterruptedException e) {
                terminate = true;
            }
            loopcount++;
        }
        parser.CloseDumpReader();
    }

    public void ReportUpdate(final VescStatus status)
    {
        Handler mainHandler = new Handler(mService.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mService.onVescStatusUpdate(status);
            }
        });
    }

    public void ReportFailure(final int errorCode)
    {
        Handler mainHandler = new Handler(mService.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mService.onParserThreadFailure(errorCode);
            }
        });
    }

    public void SetPaused(boolean paused)
    {
        synchronized (lock)
        {
            this.paused = paused;
        }
    }

    public boolean IsPaused()
    {
            return paused;
    }
}
