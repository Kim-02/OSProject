google.charts.load('current', { packages: ['timeline', 'corechart'] });
google.charts.setOnLoadCallback(init);

let processes = [];

function init() {
    document.getElementById('addProc').addEventListener('click', addProcess);
    document.getElementById('runSim').addEventListener('click', drawTimeline);
}

function addProcess() {
    const name = document.getElementById('procName').value.trim();
    const at = parseInt(document.getElementById('arrivalTime').value, 10);
    const bt = parseInt(document.getElementById('remainTime').value, 10);
    if (!name || isNaN(at) || isNaN(bt)) return alert('모든 값을 올바르게 입력하세요.');
    processes.push({ processName: name, arrivalTime: at, remainTime: bt });
    renderProcessTable();
}

function renderProcessTable() {
    const tbody = document.querySelector('#procTable tbody');
    tbody.innerHTML = '';
    processes.forEach((p, i) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
      <td>${p.processName}</td>
      <td>${p.arrivalTime}</td>
      <td>${p.remainTime}</td>
      <td><button data-i="${i}">삭제</button></td>
    `;
        tbody.appendChild(tr);
    });
    tbody.querySelectorAll('button').forEach(btn =>
        btn.addEventListener('click', e => {
            processes.splice(+e.target.dataset.i, 1);
            renderProcessTable();
        })
    );
}

async function drawTimeline() {
    if (processes.length === 0) return alert('프로세스를 하나 이상 추가하세요.');
    const alg = document.getElementById('algorithm').value;
    const pCount = parseInt(document.getElementById('pCount').value, 10);
    const eCount = parseInt(document.getElementById('eCount').value, 10);
    const processorList = [
        ...Array(pCount).fill('P'),
        ...Array(eCount).fill('E')
    ];

    const res = await fetch('/api/schedule/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ algorithm: alg, processorList, processList: processes })
    });
    const { data } = await res.json();

    // Gantt 차트
    const container = document.getElementById('chart_div');
    const chart = new google.visualization.Timeline(container);
    const dt = new google.visualization.DataTable();
    dt.addColumn({ type: 'string', id: 'Processor' });
    dt.addColumn({ type: 'string', id: 'Process' });
    dt.addColumn({ type: 'number', id: 'Start' });
    dt.addColumn({ type: 'number', id: 'End' });

    data.processorList.forEach((proc, pi) => {
        let current = null, start = null;
        for (let t = 0; t < data.totalClocks; t++) {
            const name = data.dataMatrix[t][pi] || '';
            if (name && current === null) { current = name; start = t; }
            else if (name !== current) {
                if (current) dt.addRow([proc, current, start, t]);
                current = name || null;
                start = name ? t : null;
            }
        }
        if (current) dt.addRow([proc, current, start, data.totalClocks]);
    });
    chart.draw(dt);

    // 전력 라인 차트
    const pdiv = document.getElementById('power_div');
    const pChart = new google.visualization.LineChart(pdiv);
    const pd = new google.visualization.DataTable();
    pd.addColumn('number', 'Clock');
    data.processorList.forEach(p => pd.addColumn('number', p));
    for (let t = 0; t < data.totalClocks; t++) {
        pd.addRow([t, ...data.powerMatrix[t]]);
    }
    pChart.draw(pd, { title: '클럭별 전력 소모량', curveType: 'function' });
}
